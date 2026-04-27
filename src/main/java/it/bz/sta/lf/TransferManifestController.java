package it.bz.sta.lf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bz.sta.lf.dto.TransferManifestDto;
import it.bz.sta.lf.dto.TransferManifestItemDto;
import it.bz.sta.lf.storage.S3StorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/comune-transfers")
public class TransferManifestController {
    public static final long MAX_SIGNATURE_BYTES = 2L * 1024 * 1024;

    private final TransferManifestRepository manifests;
    private final DepotRepository depots;
    private final ItemRepository items;
    private final ItemPhotoRepository itemPhotos;
    private final S3StorageService storage;
    private final AuditService audits;
    private final CompanyAccessService companyAccess;
    private final TransferManifestPdfService pdfService;
    private final ObjectMapper objectMapper;

    public TransferManifestController(
            TransferManifestRepository manifests,
            DepotRepository depots,
            ItemRepository items,
            ItemPhotoRepository itemPhotos,
            S3StorageService storage,
            AuditService audits,
            CompanyAccessService companyAccess,
            TransferManifestPdfService pdfService,
            ObjectMapper objectMapper
    ) {
        this.manifests = manifests;
        this.depots = depots;
        this.items = items;
        this.itemPhotos = itemPhotos;
        this.storage = storage;
        this.audits = audits;
        this.companyAccess = companyAccess;
        this.pdfService = pdfService;
        this.objectMapper = objectMapper;
    }

    public record PrepareComuneTransferReq(
            UUID depotId,
            String comuneName,
            String comuneContact,
            Integer boxesCount,
            String sealsCount,
            String preparedBy
    ) {}

    public record DigitalSignatureReq(
            String signatureDataUrl,
            String signedBy
    ) {}


    @PostMapping
    @Transactional
    public ResponseEntity<TransferManifestDto> prepareManifest(
            @RequestBody PrepareComuneTransferReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to prepare Comune transfer");
        String company = companyAccess.requireCompany(user);

        if (req == null || req.depotId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "depotId is required");
        }
        if (req.comuneName() == null || req.comuneName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "comuneName is required");
        }

        Depot depot = depots.findById(req.depotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));
        companyAccess.ensureDepotAccess(company, depot, "depot not found");

        OffsetDateTime threshold = OffsetDateTime.now().minusDays(14);

        List<Item> shelvedOld = items.findByCurrentLocation_Depot_IdAndStateAndFoundAtBefore(
                depot.getId(), Item.STATE_SHELVED, threshold);
        List<Item> onHoldOld = items.findByCurrentLocation_Depot_IdAndStateAndFoundAtBefore(
                depot.getId(), Item.STATE_ON_HOLD, threshold);

        for (Item item : shelvedOld) {
            companyAccess.ensureItemAccess(company, item, "item not found");
            item.setState(Item.STATE_READY_FOR_TRANSFER);
        }
        for (Item item : onHoldOld) {
            companyAccess.ensureItemAccess(company, item, "item not found");
            item.setState(Item.STATE_READY_FOR_TRANSFER);
        }

        List<Item> readyItems = items.findByCurrentLocation_Depot_IdAndState(
                        depot.getId(), Item.STATE_READY_FOR_TRANSFER)
                .stream()
                .filter(item -> companyAccess.canAccessItem(company, item))
                .toList();

        if (readyItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no items in READY_FOR_TRANSFER for this depot");
        }

        TransferManifest manifest = new TransferManifest();
        manifest.setId(UUID.randomUUID());
        manifest.setDepot(depot);
        manifest.setComuneName(req.comuneName());
        manifest.setComuneContact(req.comuneContact());
        manifest.setBoxesCount(req.boxesCount());
        manifest.setSealsCount(req.sealsCount());
        manifest.setPreparedBy(req.preparedBy() != null && !req.preparedBy().isBlank() ? req.preparedBy() : user);
        manifest.setPreparedAt(OffsetDateTime.now());
        manifest.setStatus("OPEN");

        List<TransferManifestItem> lines = new ArrayList<>();
        for (Item item : readyItems) {
            TransferManifestItem line = new TransferManifestItem();
            line.setId(UUID.randomUUID());
            line.setManifest(manifest);
            line.setItem(item);
            line.setShortCode(item.getId().toString().substring(0, 8));
            line.setCategoryMain(item.getCategoryMain());
            line.setCategorySub(item.getCategorySub());
            line.setFoundAt(item.getFoundAt());

            String foundPlace = null;
            if (item.getCurrentLocation() != null && item.getCurrentLocation().getDepot() != null) {
                foundPlace = item.getCurrentLocation().getDepot().getName();
            }
            line.setFoundPlace(foundPlace);

            String photoKey = null;
            var photos = itemPhotos.findByItemId(item.getId());
            if (!photos.isEmpty()) {
                photoKey = photos.get(0).getObjectKey();
            }
            line.setPhotoKey(photoKey);
            lines.add(line);
        }

        manifest.setItems(lines);
        manifests.save(manifest);

        audits.log(
                "COMUNE_TRANSFER_PREPARED",
                "TRANSFER_MANIFEST",
                manifest.getId(),
                manifest.getPreparedBy(),
                "{\"depotId\":\"" + depot.getId() + "\",\"company\":\"" + depot.getCompany() + "\",\"itemCount\":" + readyItems.size() + "}"
        );

        return ResponseEntity.status(201).body(toDto(manifest));
    }

    @PostMapping(path = "/{id}/sign", consumes = {"multipart/form-data"})
    @Transactional
    public ResponseEntity<TransferManifestDto> signManifest(
            @PathVariable("id") UUID manifestId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "signedBy", required = false) String signedBy,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws Exception {
        requireUser(user, "login required to sign Comune transfer");
        String company = companyAccess.requireCompany(user);

        TransferManifest manifest = manifests.findById(manifestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "manifest not found"));
        companyAccess.ensureManifestAccess(company, manifest, "manifest not found");
        assertSignable(manifest);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        if (file.getSize() > MAX_SIGNATURE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signature file too large");
        }

        String ext = extensionFromContentType(file.getContentType());
        String key = "comune-transfers/" + manifestId + "/signature-" + UUID.randomUUID() + ext;
        try (var in = file.getInputStream()) {
            storage.put(key, in, file.getSize(), file.getContentType());
        }

        applySigning(manifest, signedBy, user, company, key);
        return ResponseEntity.ok(toDto(manifest));
    }

    @PostMapping(path = "/{id}/sign-digital", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<TransferManifestDto> signManifestDigital(
            @PathVariable("id") UUID manifestId,
            @RequestBody DigitalSignatureReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws Exception {
        requireUser(user, "login required to sign Comune transfer");
        String company = companyAccess.requireCompany(user);

        TransferManifest manifest = manifests.findById(manifestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "manifest not found"));
        companyAccess.ensureManifestAccess(company, manifest, "manifest not found");
        assertSignable(manifest);

        if (req == null || req.signatureDataUrl() == null || req.signatureDataUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signatureDataUrl is required");
        }

        DecodedSignature decoded = decodeSignatureDataUrl(req.signatureDataUrl());
        String key = "comune-transfers/" + manifestId + "/signature-" + UUID.randomUUID() + decoded.extension();

        try (var in = new ByteArrayInputStream(decoded.bytes())) {
            storage.put(key, in, decoded.bytes().length, decoded.contentType());
        }

        applySigning(manifest, req.signedBy(), user, company, key);
        return ResponseEntity.ok(toDto(manifest));
    }

    @GetMapping(value = "/{id}/official-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadOfficialPdf(
            @PathVariable("id") UUID manifestId,
            @RequestParam(name = "lang", required = false, defaultValue = "de") String lang,
            @RequestParam(name = "download", required = false, defaultValue = "true") boolean download,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws IOException {
        return buildOfficialPdfResponse(manifestId, lang, download, user);
    }

    @GetMapping(value = "/{id}/official-pdf/view", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> viewOfficialPdf(
            @PathVariable("id") UUID manifestId,
            @RequestParam(name = "lang", required = false, defaultValue = "de") String lang,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws IOException {
        return buildOfficialPdfResponse(manifestId, lang, false, user);
    }

    @GetMapping(value = "/{id}/official-pdf/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadOfficialPdfAttachment(
            @PathVariable("id") UUID manifestId,
            @RequestParam(name = "lang", required = false, defaultValue = "de") String lang,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws IOException {
        return buildOfficialPdfResponse(manifestId, lang, true, user);
    }

    private ResponseEntity<byte[]> buildOfficialPdfResponse(
            UUID manifestId,
            String lang,
            boolean download,
            String user
    ) throws IOException {
        requireUser(user, "login required to download Comune transfer pdf");
        String company = companyAccess.requireCompany(user);

        TransferManifest manifest = manifests.findById(manifestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "manifest not found"));
        companyAccess.ensureManifestAccess(company, manifest, "manifest not found");

        String normalizedLang = TransferManifestPdfService.normalizeLang(lang);
        byte[] pdfBytes = pdfService.getOrCreateOfficialManifestPdf(manifest, normalizedLang, user);
        String filename = "comune-transfer-" + manifest.getId() + "-" + normalizedLang + ".pdf";
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + filename + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(pdfBytes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferManifestDto> getOne(
            @PathVariable("id") UUID manifestId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to view Comune transfers");
        String company = companyAccess.requireCompany(user);

        return manifests.findById(manifestId)
                .map(manifest -> {
                    companyAccess.ensureManifestAccess(company, manifest, "manifest not found");
                    return ResponseEntity.ok(toDto(manifest));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<TransferManifestDto> list(
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestParam(name = "status", required = false) String status,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to list Comune transfers");
        String company = companyAccess.requireCompany(user);
        validateDepotAccess(depotId, company);

        List<TransferManifest> src;
        if (depotId != null && status != null && !status.isBlank()) {
            src = manifests.findByDepot_IdAndStatusOrderByPreparedAtDesc(depotId, status);
        } else if (depotId != null) {
            src = manifests.findByDepot_IdOrderByPreparedAtDesc(depotId);
        } else {
            src = manifests.findAll();
        }

        return src.stream()
                .filter(manifest -> manifest.getDepot() != null && companyAccess.canAccessDepot(company, manifest.getDepot()))
                .map(this::toDto)
                .toList();
    }

    private void assertSignable(TransferManifest manifest) {
        if ("SIGNED".equalsIgnoreCase(manifest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "manifest already signed");
        }
        if (manifest.getItems() == null || manifest.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "manifest has no items");
        }
    }

    private void applySigning(TransferManifest manifest, String signedBy, String user, String company, String signatureKey) {
        manifest.setSignatureKey(signatureKey);
        manifest.setSignedBy(signedBy != null && !signedBy.isBlank() ? signedBy : user);
        manifest.setSignedAt(OffsetDateTime.now());
        manifest.setStatus("SIGNED");

        for (TransferManifestItem line : manifest.getItems()) {
            Item item = line.getItem();
            if (item == null) {
                continue;
            }
            companyAccess.ensureItemAccess(company, item, "item not found");
            String beforeState = item.getState();
            item.setState(Item.STATE_TRANSFERRED_TO_COMUNE);
            String afterState = item.getState();
            String auditPayload = toJson(Map.of(
                    "company", companyAccess.itemCompany(item),
                    "before", Map.of("state", beforeState),
                    "after", Map.of("state", afterState)
            ));
            audits.log(
                    "ITEM_TRANSFERRED_TO_COMUNE",
                    "ITEM",
                    item.getId(),
                    manifest.getSignedBy(),
                    auditPayload
            );
        }

        String manifestAuditPayload = toJson(Map.of(
                "signatureKey", signatureKey,
                "company", manifest.getDepot().getCompany(),
                "itemCount", manifest.getItems().size()
        ));
        audits.log(
                "COMUNE_TRANSFER_SIGNED",
                "TRANSFER_MANIFEST",
                manifest.getId(),
                manifest.getSignedBy(),
                manifestAuditPayload
        );
    }

    private static DecodedSignature decodeSignatureDataUrl(String dataUrl) {
        String normalized = dataUrl.trim();
        if (!normalized.startsWith("data:")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signatureDataUrl must be a data URL");
        }

        int commaIndex = normalized.indexOf(',');
        if (commaIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid signatureDataUrl");
        }

        String meta = normalized.substring(5, commaIndex).toLowerCase(Locale.ROOT);
        if (!meta.contains(";base64")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signatureDataUrl must use base64 encoding");
        }

        String contentType = meta.substring(0, meta.indexOf(';'));
        String extension = extensionFromContentType(contentType);

        try {
            byte[] bytes = Base64.getDecoder().decode(normalized.substring(commaIndex + 1));
            if (bytes.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signature image is empty");
            }
            if (bytes.length > MAX_SIGNATURE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signature image too large");
            }
            return new DecodedSignature(bytes, contentType, extension);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid signatureDataUrl base64 payload");
        }
    }

    private static String extensionFromContentType(String contentType) {
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signature content type is required");
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported signature content type");
        };
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "could not write audit payload");
        }
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

    private TransferManifestDto toDto(TransferManifest manifest) {
        String signatureUrl = null;
        if (manifest.getSignatureKey() != null && !manifest.getSignatureKey().isBlank()) {
            try {
                signatureUrl = storage.presignGet(manifest.getSignatureKey(), Duration.ofHours(1));
            } catch (Exception ignored) {
                signatureUrl = null;
            }
        }

        List<TransferManifestItemDto> itemsDto = new ArrayList<>();
        if (manifest.getItems() != null) {
            for (TransferManifestItem line : manifest.getItems()) {
                String photoUrl = null;
                if (line.getPhotoKey() != null && !line.getPhotoKey().isBlank()) {
                    try {
                        photoUrl = storage.presignGet(line.getPhotoKey(), Duration.ofHours(1));
                    } catch (Exception ignored) {
                        photoUrl = null;
                    }
                }
                itemsDto.add(TransferManifestItemDto.from(line, photoUrl));
            }
        }

        return TransferManifestDto.from(manifest, signatureUrl, itemsDto);
    }

    private record DecodedSignature(byte[] bytes, String contentType, String extension) {}
}
