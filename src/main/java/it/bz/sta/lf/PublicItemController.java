package it.bz.sta.lf;

import it.bz.sta.lf.dto.ClaimDto;
import it.bz.sta.lf.dto.PublicItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/items")
public class PublicItemController {

    private static final Logger log = LoggerFactory.getLogger(PublicItemController.class);

    private final ItemRepository items;
    private final ItemDocumentRepository docs;
    private final IdMatchRateLimiter rateLimiter;
    private final ClaimRepository claims;
    private final AuditService audits;

    public PublicItemController(
            ItemRepository items,
            ItemDocumentRepository docs,
            IdMatchRateLimiter rateLimiter,
            ClaimRepository claims,
            AuditService audits
    ) {
        this.items = items;
        this.docs = docs;
        this.rateLimiter = rateLimiter;
        this.claims = claims;
        this.audits = audits;
    }

    // ==== 1) Public ID-match request body ====
    public record IdMatchRequest(
            String docType,       // e.g. "ITALIAN_ID"
            String docBirthdate,  // "YYYY-MM-DD" or null
            String docNumber      // e.g. "AA12345BB"
    ) {}

    // ==== 2) Public "start claim" request body ====
    public record PublicCreateClaimReq(
            String passengerName,
            String passengerEmail,
            String passengerPhone,
            String narrative
    ) {}

    // ==== 3) Public search (text/date/depot) ====
    @GetMapping("/search")
    public List<PublicItemDto> search(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "depotId", required = false) UUID depotId
    ) {
        OffsetDateTime fromTs = null, toTs = null;
        try { if (from != null && !from.isBlank()) fromTs = OffsetDateTime.parse(from); } catch (Exception ignored) {}
        try { if (to   != null && !to.isBlank())   toTs   = OffsetDateTime.parse(to);   } catch (Exception ignored) {}

        List<Item> list = items.search(null, depotId);

        // Exclude archived states
        list = list.stream()
                .filter(i -> !Item.STATE_RETURNED.equals(i.getState())
                        && !Item.STATE_TRANSFERRED_TO_COMUNE.equals(i.getState()))
                .toList();

        if (fromTs != null) {
            OffsetDateTime f = fromTs;
            list = list.stream()
                    .filter(i -> i.getFoundAt() != null && !i.getFoundAt().isBefore(f))
                    .toList();
        }
        if (toTs != null) {
            OffsetDateTime t = toTs;
            list = list.stream()
                    .filter(i -> i.getFoundAt() != null && !i.getFoundAt().isAfter(t))
                    .toList();
        }

        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            list = list.stream()
                    .filter(i -> i.getDescription() != null
                            && i.getDescription().toLowerCase().contains(needle))
                    .toList();
        }

        return list.stream().map(PublicItemDto::from).toList();
    }

    // ==== 4) Public get-one (redacted) ====
    @GetMapping("/{id}")
    public PublicItemDto getOne(@PathVariable("id") UUID id) {
        Item item = items.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        // Hide archived items from public as well
        if (Item.STATE_RETURNED.equals(item.getState()) ||
                Item.STATE_TRANSFERRED_TO_COMUNE.equals(item.getState())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found");
        }

        return PublicItemDto.from(item);
    }

    // ==== 5) Public ID-card match (requires login + rate-limited) ====
    @PostMapping("/id-match")
    public List<PublicItemDto> matchById(
            @RequestBody IdMatchRequest req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to search by ID document");
        }

        // Rate limiting per X-User
        rateLimiter.checkAllowed(user);

        if (req == null || req.docNumber() == null || req.docNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "docNumber is required");
        }

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

        // --- Normalization MUST match ItemDocumentController ---
        String digits = req.docNumber().replaceAll("\\D", "");
        String tokenBase = digits + "|" + (birthdate != null ? birthdate.toString() : "");
        String hash = sha256Hex(tokenBase);

        List<ItemDocument> matches = docs.findByDocMatchHash(hash);

        // Map to distinct items
        List<Item> candidateItems = matches.stream()
                .map(ItemDocument::getItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Item::getId,
                        i -> i,
                        (a, b) -> a
                ))
                .values()
                .stream()
                .toList();

        // Filter out archived items for public
        candidateItems = candidateItems.stream()
                .filter(i -> !Item.STATE_RETURNED.equals(i.getState())
                        && !Item.STATE_TRANSFERRED_TO_COMUNE.equals(i.getState()))
                .toList();

        // Security-friendly log: no docNumber, no digits
        log.info("ID-MATCH attempt by user={} docType={} birthdateProvided={} matches={}",
                user,
                req.docType(),
                (birthdate != null),
                candidateItems.size()
        );

        return candidateItems.stream().map(PublicItemDto::from).toList();
    }

    // ==== 6) Public "start claim for this item" (requires login) ====
    @PostMapping("/{id}/claim")
    public ClaimDto startClaim(
            @PathVariable("id") UUID itemId,
            @RequestBody PublicCreateClaimReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to start a claim");
        }

        Item item = items.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        // Do not allow claims on archived items
        if (Item.STATE_RETURNED.equals(item.getState()) ||
                Item.STATE_TRANSFERRED_TO_COMUNE.equals(item.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item is not claimable anymore");
        }

        // Basic validation
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (req.passengerName() == null || req.passengerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passengerName is required");
        }
        if (req.passengerEmail() == null || req.passengerEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passengerEmail is required");
        }

        // Create claim
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

        // NEW: link to public user + generate reference code
        c.setPublicUserId(user);
        String refCode = c.getId().toString().substring(0, 8).toUpperCase(); // e.g. "A1B2C3D4"
        c.setPublicReferenceCode(refCode);

        Claim saved = claims.save(c);

        // Audit as "public-created" claim
        audits.log(
                "CLAIM_CREATED_PUBLIC",
                "CLAIM",
                saved.getId(),
                user,
                "{\"itemId\":\"" + item.getId() + "\",\"publicRef\":\"" + refCode + "\"}"
        );

        log.info("Public claim created by user={} for item={} claimId={} ref={}",
                user, item.getId(), saved.getId(), refCode);

        return ClaimDto.from(saved);
    }


    // ==== 7) Local SHA helper (same as ItemDocumentController) ====
    private static String sha256Hex(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
