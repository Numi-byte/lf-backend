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
    private final CompanyAccessService companyAccess;
    private final ClaimEmailNotificationService claimEmailNotificationService;

    public ClaimController(
            ClaimRepository claims,
            ItemRepository items,
            ItemDocumentRepository itemDocuments,
            AuditService audits,
            CompanyAccessService companyAccess,
            ClaimEmailNotificationService claimEmailNotificationService
    ) {
        this.claims = claims;
        this.items = items;
        this.itemDocuments = itemDocuments;
        this.audits = audits;
        this.companyAccess = companyAccess;
        this.claimEmailNotificationService = claimEmailNotificationService;
    }

    // ----- Request DTOs -----
    public record CreateClaim(
            UUID itemId,
            String passengerName,
            String passengerEmail,
            String passengerPhone,
            String narrative,
            String docNumber,
            String docBirthdate
    ) {}

    public record ApproveReq(
            String method,
            Integer feeCents
    ) {}

    @GetMapping
    public List<ClaimDto> list(
            @RequestParam(name = "itemId", required = false) UUID itemId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to list claims");
        String company = companyAccess.requireCompany(user);

        if (itemId != null) {
            Item item = items.findById(itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
            companyAccess.ensureItemAccess(company, item, "item not found");
        }

        List<Claim> src = (itemId == null) ? claims.findAll() : claims.findByItemId(itemId);
        return src.stream()
                .filter(claim -> claim.getItem() != null && companyAccess.canAccessItem(company, claim.getItem()))
                .map(ClaimDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimDto> get(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to view claims");
        String company = companyAccess.requireCompany(user);

        return claims.findById(id)
                .map(claim -> {
                    companyAccess.ensureClaimAccess(company, claim, "claim not found");
                    return ResponseEntity.ok(ClaimDto.from(claim));
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping
    public ResponseEntity<ClaimDto> create(
            @RequestBody CreateClaim req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to create claims");
        String company = companyAccess.requireCompany(user);

        if (req == null || req.itemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemId is required");
        }
        if (req.passengerName() == null || req.passengerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passengerName is required");
        }
        if (req.passengerEmail() == null || req.passengerEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passengerEmail is required");
        }

        String normalizedEmail = req.passengerEmail().trim().toLowerCase();

        Item item = items.findById(req.itemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        companyAccess.ensureItemAccess(company, item, "item not found");

        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setItem(item);
        claim.setPassengerName(req.passengerName());
        claim.setPassengerEmail(normalizedEmail);
        claim.setPassengerPhone(req.passengerPhone());
        claim.setNarrative(req.narrative());
        claim.setStatus("new");
        claim.setSubmittedAt(OffsetDateTime.now());
        claim.setUpdatedAt(OffsetDateTime.now());

        String refCode = claim.getId().toString().substring(0, 8).toUpperCase();
        claim.setPublicReferenceCode(refCode);

        String docMatchStatus = "NOT_PROVIDED";
        if (req.docNumber() != null && !req.docNumber().isBlank()) {
            LocalDate birthdate = null;
            if (req.docBirthdate() != null && !req.docBirthdate().isBlank()) {
                try {
                    birthdate = LocalDate.parse(req.docBirthdate());
                } catch (DateTimeParseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid docBirthdate, expected YYYY-MM-DD");
                }
            }

            String digits = req.docNumber().replaceAll("\\D", "");
            String tokenBase = digits + "|" + (birthdate != null ? birthdate.toString() : "");
            String claimHash = sha256Hex(tokenBase);

            List<ItemDocument> docsForItem = itemDocuments.findByItem_Id(item.getId());
            if (docsForItem.isEmpty()) {
                docMatchStatus = "NO_DOCUMENT_ON_ITEM";
            } else {
                boolean match = docsForItem.stream().anyMatch(d -> claimHash.equals(d.getDocMatchHash()));
                docMatchStatus = match ? "FULL_MATCH" : "NO_MATCH";
            }
        }

        Claim saved = claims.save(claim);
        claimEmailNotificationService.sendClaimCreatedNotifications(saved);

        // Store result only in audit details (no need to persist docNumber)
        audits.log(
                "CLAIM_CREATED",
                "CLAIM",
                saved.getId(),
                user,
                "{"
                        + "\"itemId\":\"" + item.getId() + "\","
                        + "\"company\":\"" + companyAccess.itemCompany(item) + "\","
                        + "\"publicRef\":\"" + refCode + "\","
                        + "\"docMatchStatus\":\"" + docMatchStatus + "\""
                        + "}"
        );

        return ResponseEntity.status(201).body(ClaimDto.from(saved));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ClaimDto> approve(
            @PathVariable("id") UUID id,
            @RequestBody ApproveReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to approve claims");
        String company = companyAccess.requireCompany(user);

        Claim claim = claims.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "claim not found"));
        companyAccess.ensureClaimAccess(company, claim, "claim not found");

        if (req == null || req.method() == null || req.method().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "method is required");
        }

        String previousStatus = claim.getStatus();
        Item item = claim.getItem();
        String previousItemState = item == null ? null : item.getState();

        claim.setStatus("approved");
        claim.setMethod(req.method());
        claim.setFeeCents(req.feeCents() == null ? 0 : req.feeCents());
        claim.setUpdatedAt(OffsetDateTime.now());

        if (item != null) {
            String before = "{\"state\":\"" + item.getState() + "\"}";
            item.setState(Item.STATE_ON_HOLD);
            String after = "{\"state\":\"" + item.getState() + "\"}";

            audits.log(
                    "CLAIM_APPROVED",
                    "CLAIM",
                    claim.getId(),
                    user,
                    "{\"itemId\":\"" + item.getId() + "\",\"company\":\"" + companyAccess.itemCompany(item) + "\",\"before\":" + before + ",\"after\":" + after + "}"
            );
        } else {
            audits.log("CLAIM_APPROVED", "CLAIM", claim.getId(), user, null);
        }

        String currentItemState = item == null ? null : item.getState();
        if (statusChanged(previousStatus, claim.getStatus()) || statusChanged(previousItemState, currentItemState)) {
            claimEmailNotificationService.sendClaimUpdatedNotifications(claim, previousStatus, previousItemState);
        }

        return ResponseEntity.ok(ClaimDto.from(claim));
    }

    @PostMapping("/{id}/close")
    @Transactional
    public ResponseEntity<ClaimDto> close(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to close claims");
        String company = companyAccess.requireCompany(user);

        Claim claim = claims.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "claim not found"));
        companyAccess.ensureClaimAccess(company, claim, "claim not found");

        String previousStatus = claim.getStatus();
        Item item = claim.getItem();
        String previousItemState = item == null ? null : item.getState();

        claim.setStatus("closed");
        claim.setUpdatedAt(OffsetDateTime.now());

        audits.log("CLAIM_CLOSED", "CLAIM", claim.getId(), user, null);

        String currentItemState = item == null ? null : item.getState();
        if (statusChanged(previousStatus, claim.getStatus()) || statusChanged(previousItemState, currentItemState)) {
            claimEmailNotificationService.sendClaimUpdatedNotifications(claim, previousStatus, previousItemState);
        }

        return ResponseEntity.ok(ClaimDto.from(claim));
    }

    private static boolean statusChanged(String before, String after) {
        return before == null ? after != null : !before.equals(after);
    }

    private static void requireUser(String user, String message) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
    }

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
