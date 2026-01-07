package it.bz.sta.lf;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
public class ItemDocumentController {

    private final ItemRepository items;
    private final ItemDocumentRepository docs;
    private final AuditService audits;

    public ItemDocumentController(ItemRepository items, ItemDocumentRepository docs, AuditService audits) {
        this.items = items;
        this.docs = docs;
        this.audits = audits;
    }

    public record DocumentReq(
            String docType,       // e.g. "ITALIAN_ID"
            String docName,       // name on the document
            String docBirthdate,  // "YYYY-MM-DD" or null
            String docIssuer,     // Comune, etc.
            String docNumber      // e.g. "AA12345BB"
    ) {}

    public record DocumentDto(
            UUID id,
            String docType,
            String docName,
            String docBirthdate,
            String docIssuer,
            String docNumberMasked
    ) {
        public static DocumentDto from(ItemDocument d) {
            String birth = d.getDocBirthdate() != null ? d.getDocBirthdate().toString() : null;
            return new DocumentDto(
                    d.getId(),
                    d.getDocType(),
                    d.getDocName(),
                    birth,
                    d.getDocIssuer(),
                    maskDocNumber(d.getDocNumberFull())
            );
        }

        private static String maskDocNumber(String full) {
            if (full == null || full.isBlank()) return null;
            if (full.length() <= 4) return "****";

            int len = full.length();
            String prefix = full.substring(0, 2);
            String suffix = full.substring(len - 2);
            return prefix + "*".repeat(len - 4) + suffix;
        }
    }

    /**
     * Create or replace document info for an item.
     * POST /items/{id}/id-document
     */
    @PostMapping("/{id}/id-document")
    public ResponseEntity<DocumentDto> createOrUpdate(
            @PathVariable("id") UUID itemId,
            @RequestBody DocumentReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to register document info");
        }

        if (req == null || req.docType() == null || req.docType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "docType is required");
        }
        if (req.docNumber() == null || req.docNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "docNumber is required");
        }

        Item item = items.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        LocalDate birthdate = null;
        if (req.docBirthdate() != null && !req.docBirthdate().isBlank()) {
            try {
                birthdate = LocalDate.parse(req.docBirthdate());
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid docBirthdate, expected YYYY-MM-DD");
            }
        }

        // MUST match PublicItemController normalization
        String digits = req.docNumber().replaceAll("\\D", "");
        String tokenBase = digits + "|" + (birthdate != null ? birthdate.toString() : "");
        String hash = sha256Hex(tokenBase);

        // Keep only one doc per item
        List<ItemDocument> existing = docs.findByItem_Id(itemId);
        if (!existing.isEmpty()) {
            docs.deleteAllInBatch(existing);
        }

        ItemDocument d = new ItemDocument();
        d.setId(UUID.randomUUID());
        d.setItem(item);
        d.setDocType(req.docType());
        d.setDocName(req.docName());
        d.setDocBirthdate(birthdate);
        d.setDocIssuer(req.docIssuer());
        d.setDocNumberFull(req.docNumber());   // NOTE: stores full value (see note below)
        d.setDocMatchHash(hash);

        ItemDocument saved = docs.save(d);

        audits.log(
                "ITEM_DOCUMENT_SET",
                "ITEM",
                item.getId(),
                user,
                "{\"docType\":\"" + req.docType() + "\",\"birthdateProvided\":" + (birthdate != null) + "}"
        );

        return ResponseEntity.status(201).body(DocumentDto.from(saved));
    }

    /**
     * List document info for an item (internal-only; masked number).
     * GET /items/{id}/id-document
     */
    @GetMapping("/{id}/id-document")
    public List<DocumentDto> list(
            @PathVariable("id") UUID itemId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to view document info");
        }

        items.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        return docs.findByItem_Id(itemId).stream()
                .map(DocumentDto::from)
                .toList();
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
