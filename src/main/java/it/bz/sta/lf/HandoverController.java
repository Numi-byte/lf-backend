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
    private final DepotRepository depots;
    private final AuditService audits;
    private final S3StorageService storage;
    private final CompanyAccessService companyAccess;

    public HandoverController(
            ItemRepository items,
            HandoverRepository handovers,
            DepotRepository depots,
            AuditService audits,
            S3StorageService storage,
            CompanyAccessService companyAccess
    ) {
        this.items = items;
        this.handovers = handovers;
        this.depots = depots;
        this.audits = audits;
        this.storage = storage;
        this.companyAccess = companyAccess;
    }

    // Request body for creating a handover (metadata only)
    public record HandoverReq(
            String type,
            String personName,
            String documentType,
            String documentNumber,
            String comuneName,
            String comuneReference,
            String notes,
            String attachmentKey
    ) {}

    @PostMapping("/items/{id}/handover")
    public ResponseEntity<HandoverDto> handover(
            @PathVariable("id") UUID itemId,
            @RequestBody HandoverReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to create handover");
        String company = companyAccess.requireCompany(user);

        Item item = items.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        companyAccess.ensureItemAccess(company, item, "item not found");


        String type = (req != null && req.type() != null) ? req.type().toUpperCase() : "PERSON";
        if (!type.equals("PERSON") && !type.equals("COMUNE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be PERSON or COMUNE");
        }
        if (type.equals("PERSON")) {
            if (req == null || req.personName() == null || req.personName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personName is required for PERSON handover");
            }
        } else { if (req == null || req.comuneName() == null || req.comuneName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "comuneName is required for COMUNE handover");
            }
        }

        Handover handover = new Handover();
        handover.setId(UUID.randomUUID());
        handover.setItem(item);

        if (item.getCurrentLocation() != null && item.getCurrentLocation().getDepot() != null) {
            handover.setDepotId(item.getCurrentLocation().getDepot().getId());
        }

        handover.setType(type);
        handover.setPerformedBy(user);

        if (type.equals("PERSON")) {
            handover.setPersonName(req.personName());
            handover.setDocumentType(req.documentType());
            handover.setDocumentNumber(req.documentNumber());
        } else {
            handover.setComuneName(req.comuneName());
            handover.setComuneReference(req.comuneReference());
        }

        handover.setNotes(req != null ? req.notes() : null);
        handover.setAttachmentKey(req != null ? req.attachmentKey() : null);
        handover.setCreatedAt(OffsetDateTime.now());

        handovers.save(handover);

        String before = "{\"state\":\"" + item.getState() + "\"}";
        switch (type) {
            case "PERSON" -> item.setState(Item.STATE_RETURNED);
            case "COMUNE" -> item.setState(Item.STATE_TRANSFERRED_TO_COMUNE);
        }

        String after = "{\"state\":\"" + item.getState() + "\"}";

        audits.log(
                "ITEM_HANDOVER",
                "HANDOVER",
                handover.getId(),
                user,
                "{"
                        + "\"itemId\":\"" + item.getId() + "\","
                        + "\"type\":\"" + type + "\","
                        + "\"company\":\"" + companyAccess.itemCompany(item) + "\","
                        + "\"before\":" + before + ","
                        + "\"after\":" + after
                        + "}"
        );

        return ResponseEntity.status(201).body(toDto(handover));
    }


    @PostMapping(path = "/handovers/{id}/docs", consumes = {"multipart/form-data"})
    public ResponseEntity<HandoverDto> uploadDocs(
            @PathVariable("id") UUID handoverId,
            @RequestParam("front") MultipartFile front,
            @RequestParam("back") MultipartFile back,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws Exception {
        requireUser(user, "login required to upload handover docs");
        String company = companyAccess.requireCompany(user);

        Handover handover = handovers.findById(handoverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "handover not found"));
        companyAccess.ensureHandoverAccess(company, handover, "handover not found");

        if (front == null || front.isEmpty() || back == null || back.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "both front and back files are required");
        }

        String base = "handover-docs/" + handoverId + "/";
        String frontKey  = base + "front-" + UUID.randomUUID();
        String backKey  = base + "back-" + UUID.randomUUID();

        try (var in = front.getInputStream()) {
            storage.put(frontKey, in, front.getSize(), front.getContentType());
        }
        try (var in = back.getInputStream()) {
            storage.put(backKey, in, back.getSize(), back.getContentType());
        }

        handover.setDocFrontKey(frontKey);
        handover.setDocBackKey(backKey);
        handovers.save(handover);

        return ResponseEntity.ok(toDto(handover));
    }

    @GetMapping("/items/{id}/handovers")
    public List<HandoverDto> listForItem(
            @PathVariable("id") UUID itemId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to view handovers");
        String company = companyAccess.requireCompany(user);

        Item item = items.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        companyAccess.ensureItemAccess(company, item, "item not found");

        return handovers.findByItemId(itemId).stream()
                .filter(handover -> handover.getItem() != null && companyAccess.canAccessItem(company, handover.getItem()))
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/handovers")
    public List<HandoverDto> search(
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "text", required = false) String text,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to search handovers");
        String company = companyAccess.requireCompany(user);
        validateDepotAccess(depotId, company);

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

        if (fromTs == null) {
            fromTs = OffsetDateTime.parse("2000-01-01T00:00:00Z");
        }
        if (toTs == null) {
            toTs = OffsetDateTime.parse("2100-01-01T00:00:00Z");
        }

        return handovers.search(depotId, fromTs, toTs, text)
                .stream()
                .filter(handover -> handover.getItem() != null && companyAccess.canAccessItem(company, handover.getItem()))
                .map(this::toDto)
                .toList();
    }

    private void validateDepotAccess(UUID depotId, String company) {
        if (depotId == null) {
            return;
        }
        Depot depot = depots.findById(depotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));
        companyAccess.ensureDepotAccess(company, depot, "depot not found");
    }

    private static void requireUser(String user, String message) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
    }

    private HandoverDto toDto(Handover handover) {
        String frontUrl  = null;
        String backUrl  = null;
        try {
            if (handover.getDocFrontKey() != null && !handover.getDocFrontKey().isBlank()) {
                frontUrl = storage.presignGet(handover.getDocFrontKey(), Duration.ofHours(1));
            }
            if (handover.getDocBackKey() != null && !handover.getDocBackKey().isBlank()) {
                backUrl = storage.presignGet(handover.getDocBackKey(), Duration.ofHours(1));
            }
        } catch (Exception ignored) {

        }
        return HandoverDto.from(handover, frontUrl, backUrl);
    }
}
