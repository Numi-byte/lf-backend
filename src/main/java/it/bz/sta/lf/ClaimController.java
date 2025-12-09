package it.bz.sta.lf;

import it.bz.sta.lf.dto.ClaimDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimRepository claims;
    private final ItemRepository items;
    private final ItemDocumentRepository itemDocuments;
    private final AuditService audits;

    public ClaimController(
            ClaimRepository claims,
            ItemRepository items,
            ItemDocumentRepository itemDocuments,
            AuditService audits
    ) {
        this.claims = claims;
        this.items = items;
        this.itemDocuments = itemDocuments;
        this.audits = audits;
    }

    // ----- Request DTOs -----
    public record CreateClaim(
            UUID itemId,
            String passengerName,
            String passengerEmail,
            String passengerPhone,
            String narrative,
            String docNumber,      // optional: ID / document number provided by claimant
            String docBirthdate    // optional: YYYY-MM-DD
    ) {}

    public record ApproveReq(
            String method,
            Integer feeCents
    ) {}

    // ----- List & get (login required) -----
    @GetMapping
    public List<ClaimDto> list(
            @RequestParam(name = "itemId", required = false) UUID itemId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to list claims");
        }

        List<Claim> src = (itemId == null) ? claims.findAll() : claims.findByItemId(itemId);
        return src.stream().map(ClaimDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimDto> get(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to view claims");
        }

        return claims.findById(id)
                .map(c -> ResponseEntity.ok(ClaimDto.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ----- Create claim (login required, with optional ID-match logic) -----
    @PostMapping
    public ResponseEntity<ClaimDto> create(
            @RequestBody CreateClaim req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to create claims");
        }

        if (req == null || req.itemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemId is required");
        }

        Item item = items.findById(req.itemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        Claim c = new Claim();
        c.setId(UUID.randomUUID());
        c.setItem(item);
        c.setPassengerName(req.passengerName());
        c.setPassengerEmail(req.passengerEmail());
        c.setPassengerPhone(req.passengerPhone());
        c.setNarrative(req.narrative());
        c.setStatus("new");
        c.setSubmittedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());

        Claim saved = claims.save(c);

        // --- Wallet / ID-card match logic (optional) ---
        String docMatchStatus = "NOT_PROVIDED";

        if (req.docNumber() != null && !req.docNumber().isBlank()) {
            LocalDate birthdate = null;
            if (req.docBirthdate() != null && !req.docBirthdate().isBlank()) {
                try {
                    birthdate = LocalDate.parse(req.docBirthdate());
                } catch (DateTimeParseException e) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "invalid docBirthdate, expected YYYY-MM-DD"
                    );
                }
            }

            // Normalize same as ItemDocumentController:
            // digits(docNumber) + "|" + birthdate
            String digits = req.docNumber().replaceAll("\\D", "");
            String tokenBase = digits + "|" + (birthdate != null ? birthdate.toString() : "");
            String claimHash = sha256Hex(tokenBase);

            List<ItemDocument> docsForItem = itemDocuments.findByItem_Id(item.getId());
            if (docsForItem.isEmpty()) {
                docMatchStatus = "NO_DOCUMENT_ON_ITEM";
            } else {
                boolean match = docsForItem.stream()
                        .anyMatch(d -> claimHash.equals(d.getDocMatchHash()));
                docMatchStatus = match ? "FULL_MATCH" : "NO_MATCH";
            }
        }

        // Store result only in audit details (no need to persist docNumber)
        audits.log(
                "CLAIM_CREATED",
                "CLAIM",
                saved.getId(),
                user,
                "{"
                        + "\"itemId\":\"" + item.getId() + "\","
                        + "\"docMatchStatus\":\"" + docMatchStatus + "\""
                        + "}"
        );

        return ResponseEntity.status(201).body(ClaimDto.from(saved));
    }

    // ----- Approve claim → put item ON_HOLD (login required) -----
    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ClaimDto> approve(
            @PathVariable("id") UUID id,
            @RequestBody ApproveReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to approve claims");
        }

        Claim c = claims.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "claim not found"));

        if (req == null || req.method() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "method is required");
        }

        c.setStatus("approved");
        c.setMethod(req.method());
        c.setFeeCents(req.feeCents() == null ? 0 : req.feeCents());
        c.setUpdatedAt(OffsetDateTime.now());

        // 🔹 Link to item → set ON_HOLD
        Item item = c.getItem();
        if (item != null) {
            String before = "{\"state\":\"" + item.getState() + "\"}";
            item.setState(Item.STATE_ON_HOLD);
            String after = "{\"state\":\"" + item.getState() + "\"}";

            audits.log(
                    "CLAIM_APPROVED",
                    "CLAIM",
                    c.getId(),
                    user,
                    "{\"itemId\":\"" + item.getId() + "\",\"before\":" + before + ",\"after\":" + after + "}"
            );
        } else {
            audits.log("CLAIM_APPROVED", "CLAIM", c.getId(), user, null);
        }

        return ResponseEntity.ok(ClaimDto.from(c));
    }

    // ----- Close claim (manual, login required) -----
    @PostMapping("/{id}/close")
    @Transactional
    public ResponseEntity<ClaimDto> close(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to close claims");
        }

        Claim c = claims.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "claim not found"));

        c.setStatus("closed");
        c.setUpdatedAt(OffsetDateTime.now());

        audits.log("CLAIM_CLOSED", "CLAIM", c.getId(), user, null);

        return ResponseEntity.ok(ClaimDto.from(c));
    }

    // ----- helper: SHA-256 hex -----
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute SHA-256", e);
        }
    }
}
