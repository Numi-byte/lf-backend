package it.bz.sta.lf;

import it.bz.sta.lf.dto.HandoverDto;
import it.bz.sta.lf.storage.S3StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class HandoverController {

    private final ItemRepository items;
    private final HandoverRepository handovers;
    private final AuditService audits;
    private final S3StorageService storage;

    public HandoverController(
            ItemRepository items,
            HandoverRepository handovers,
            AuditService audits,
            S3StorageService storage
    ) {
        this.items = items;
        this.handovers = handovers;
        this.audits = audits;
        this.storage = storage;
    }

    // Request body for creating a handover (metadata only)
    public record HandoverReq(
            String type,              // PERSON | COMUNE
            String personName,
            String documentType,
            String documentNumber,
            String comuneName,
            String comuneReference,
            String notes,
            String attachmentKey
    ) {}

    /**
     * 1) Create a handover (PERSON or COMUNE).
     *    - For PERSON: we expect personName + document info.
     *    - For COMUNE: we expect comuneName (and optionally comuneReference).
     *    - Later, a second call will upload front/back document photos.
     *
     *    Also: updates Item state
     *      - PERSON  -> RETURNED
     *      - COMUNE  -> TRANSFERRED_TO_COMUNE
     */
    @PostMapping("/items/{id}/handover")
    public ResponseEntity<HandoverDto> handover(
            @PathVariable("id") UUID itemId,
            @RequestBody HandoverReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to create handover");
        }

        Item item = items.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        // Type validation
        String type = (req != null && req.type() != null) ? req.type().toUpperCase() : "PERSON";
        if (!type.equals("PERSON") && !type.equals("COMUNE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be PERSON or COMUNE");
        }

        // Required fields depending on type
        if (type.equals("PERSON")) {
            if (req == null || req.personName() == null || req.personName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personName is required for PERSON handover");
            }
        } else { // COMUNE
            if (req == null || req.comuneName() == null || req.comuneName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "comuneName is required for COMUNE handover");
            }
        }

        Handover h = new Handover();
        h.setId(UUID.randomUUID());
        h.setItem(item);

        if (item.getCurrentLocation() != null && item.getCurrentLocation().getDepot() != null) {
            h.setDepotId(item.getCurrentLocation().getDepot().getId());
        }

        h.setType(type);
        h.setPerformedBy(user);

        if (type.equals("PERSON")) {
            h.setPersonName(req.personName());
            h.setDocumentType(req.documentType());
            h.setDocumentNumber(req.documentNumber());
        } else {
            h.setComuneName(req.comuneName());
            h.setComuneReference(req.comuneReference());
        }

        h.setNotes(req != null ? req.notes() : null);
        h.setAttachmentKey(req != null ? req.attachmentKey() : null);
        h.setCreatedAt(OffsetDateTime.now());

        handovers.save(h);

        // 🔹 set item state based on type
        String before = "{\"state\":\"" + item.getState() + "\"}";

        switch (type) {
            case "PERSON" -> item.setState(Item.STATE_RETURNED);
            case "COMUNE" -> item.setState(Item.STATE_TRANSFERRED_TO_COMUNE);
        }

        String after  = "{\"state\":\"" + item.getState() + "\"}";

        audits.log(
                "ITEM_HANDOVER",
                "HANDOVER",
                h.getId(),
                user,
                "{"
                        + "\"itemId\":\"" + item.getId() + "\","
                        + "\"type\":\"" + type + "\","
                        + "\"before\":" + before + ","
                        + "\"after\":" + after
                        + "}"
        );

        return ResponseEntity.status(201).body(toDto(h));
    }

    /**
     * 2) Upload BOTH sides of the document for this handover.
     *
     *    POST /handovers/{id}/docs
     *    form-data:
     *      - front: file
     *      - back:  file
     */
    @PostMapping(path = "/handovers/{id}/docs", consumes = {"multipart/form-data"})
    public ResponseEntity<HandoverDto> uploadDocs(
            @PathVariable("id") UUID handoverId,
            @RequestParam("front") MultipartFile front,
            @RequestParam("back") MultipartFile back,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws Exception {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to upload handover docs");
        }

        Handover h = handovers.findById(handoverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "handover not found"));

        if (front == null || front.isEmpty() || back == null || back.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "both front and back files are required");
        }

        String base = "handover-docs/" + handoverId + "/";
        String frontKey = base + "front-" + UUID.randomUUID();
        String backKey  = base + "back-" + UUID.randomUUID();

        try (var in = front.getInputStream()) {
            storage.put(frontKey, in, front.getSize(), front.getContentType());
        }
        try (var in = back.getInputStream()) {
            storage.put(backKey, in, back.getSize(), back.getContentType());
        }

        h.setDocFrontKey(frontKey);
        h.setDocBackKey(backKey);
        handovers.save(h);

        return ResponseEntity.ok(toDto(h));
    }

    @GetMapping("/items/{id}/handovers")
    public List<HandoverDto> listForItem(
            @PathVariable("id") UUID itemId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to view handovers");
        }

        items.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        return handovers.findByItemId(itemId).stream().map(this::toDto).toList();
    }

    @GetMapping("/handovers")
    public List<HandoverDto> search(
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "text", required = false) String text,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to search handovers");
        }

        OffsetDateTime fromTs = null;
        OffsetDateTime toTs = null;

        try {
            if (from != null && !from.isBlank()) {
                fromTs = OffsetDateTime.parse(from);
            }
        } catch (Exception ignored) {}

        try {
            if (to != null && !to.isBlank()) {
                toTs = OffsetDateTime.parse(to);
            }
        } catch (Exception ignored) {}

        // If user did not pass from/to, use a very wide range
        if (fromTs == null) {
            fromTs = OffsetDateTime.parse("2000-01-01T00:00:00Z");
        }
        if (toTs == null) {
            toTs = OffsetDateTime.parse("2100-01-01T00:00:00Z");
        }

        return handovers.search(depotId, fromTs, toTs, text)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private HandoverDto toDto(Handover h) {
        String frontUrl = null;
        String backUrl  = null;
        try {
            if (h.getDocFrontKey() != null && !h.getDocFrontKey().isBlank()) {
                frontUrl = storage.presignGet(h.getDocFrontKey(), Duration.ofHours(1));
            }
            if (h.getDocBackKey() != null && !h.getDocBackKey().isBlank()) {
                backUrl = storage.presignGet(h.getDocBackKey(), Duration.ofHours(1));
            }
        } catch (Exception e) {
            // If MinIO is down, we just return null URLs instead of killing the request
        }
        return HandoverDto.from(h, frontUrl, backUrl);
    }
}
