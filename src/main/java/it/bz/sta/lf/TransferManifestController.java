package it.bz.sta.lf;

import it.bz.sta.lf.dto.TransferManifestDto;
import it.bz.sta.lf.dto.TransferManifestItemDto;
import it.bz.sta.lf.storage.S3StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comune-transfers")
public class TransferManifestController {

    private final TransferManifestRepository manifests;
    private final DepotRepository depots;
    private final ItemRepository items;
    private final ItemPhotoRepository itemPhotos;
    private final S3StorageService storage;
    private final AuditService audits;

    public TransferManifestController(
            TransferManifestRepository manifests,
            DepotRepository depots,
            ItemRepository items,
            ItemPhotoRepository itemPhotos,
            S3StorageService storage,
            AuditService audits
    ) {
        this.manifests = manifests;
        this.depots = depots;
        this.items = items;
        this.itemPhotos = itemPhotos;
        this.storage = storage;
        this.audits = audits;
    }

    // --- Request DTOs ---

    public record PrepareComuneTransferReq(
            UUID depotId,
            String comuneName,
            String comuneContact,
            Integer boxesCount,
            String sealsCount,
            String preparedBy
    ) {}

    // --- 1) Create manifest (includes auto 14-day READY_FOR_TRANSFER marking) ---

    @PostMapping
    @Transactional
    public ResponseEntity<TransferManifestDto> prepareManifest(
            @RequestBody PrepareComuneTransferReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to prepare Comune transfer");
        }

        if (req == null || req.depotId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "depotId is required");
        }
        if (req.comuneName() == null || req.comuneName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "comuneName is required");
        }

        Depot depot = depots.findById(req.depotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));

        // 1A) Auto-mark items >=14 days as READY_FOR_TRANSFER
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(14);

        List<Item> shelvedOld = items.findByCurrentLocation_Depot_IdAndStateAndFoundAtBefore(
                depot.getId(), Item.STATE_SHELVED, threshold);
        List<Item> onHoldOld = items.findByCurrentLocation_Depot_IdAndStateAndFoundAtBefore(
                depot.getId(), Item.STATE_ON_HOLD, threshold);

        for (Item it : shelvedOld) {
            it.setState(Item.STATE_READY_FOR_TRANSFER);
        }
        for (Item it : onHoldOld) {
            it.setState(Item.STATE_READY_FOR_TRANSFER);
        }

        // 1B) Collect all READY_FOR_TRANSFER items for this depot
        List<Item> readyItems = items.findByCurrentLocation_Depot_IdAndState(
                depot.getId(), Item.STATE_READY_FOR_TRANSFER);

        if (readyItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "no items in READY_FOR_TRANSFER for this depot");
        }

        // 1C) Build manifest header
        TransferManifest m = new TransferManifest();
        m.setId(UUID.randomUUID());
        m.setDepot(depot);
        m.setComuneName(req.comuneName());
        m.setComuneContact(req.comuneContact());
        m.setBoxesCount(req.boxesCount());
        m.setSealsCount(req.sealsCount());
        m.setPreparedBy(req.preparedBy() != null && !req.preparedBy().isBlank() ? req.preparedBy() : user);
        m.setPreparedAt(OffsetDateTime.now());
        m.setStatus("OPEN");

        // 1D) Add lines (short code, found date/place, small photo)
        List<TransferManifestItem> lines = new ArrayList<>();

        for (Item it : readyItems) {
            TransferManifestItem line = new TransferManifestItem();
            line.setId(UUID.randomUUID());
            line.setManifest(m);
            line.setItem(it);

            // short code = first 8 chars of itemId (can be changed later)
            String shortCode = it.getId().toString().substring(0, 8);
            line.setShortCode(shortCode);

            // category: we don't have a dedicated field yet, so leave null for now
            line.setCategoryMain(it.getCategoryMain());
            line.setCategorySub(it.getCategorySub());

            line.setFoundAt(it.getFoundAt());

            String foundPlace = null;
            if (it.getCurrentLocation() != null && it.getCurrentLocation().getDepot() != null) {
                foundPlace = it.getCurrentLocation().getDepot().getName();
            }
            line.setFoundPlace(foundPlace);

            // take first photo (if any)
            String photoKey = null;
            var photos = itemPhotos.findByItemId(it.getId());
            if (!photos.isEmpty()) {
                photoKey = photos.get(0).getObjectKey();
            }
            line.setPhotoKey(photoKey);

            lines.add(line);
        }

        m.setItems(lines);

        manifests.save(m);

        // 1E) Audit manifest creation
        String actor = m.getPreparedBy();
        audits.log(
                "COMUNE_TRANSFER_PREPARED",
                "TRANSFER_MANIFEST",
                m.getId(),
                actor,
                "{\"depotId\":\"" + depot.getId() + "\",\"itemCount\":" + readyItems.size() + "}"
        );

        return ResponseEntity.status(201).body(toDto(m));
    }

    // --- 2) Comune signs manifest + upload signed document, move items to TRANSFERRED_TO_COMUNE ---

    @PostMapping(path = "/{id}/sign", consumes = { "multipart/form-data" })
    @Transactional
    public ResponseEntity<TransferManifestDto> signManifest(
            @PathVariable("id") UUID manifestId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "signedBy", required = false) String signedBy,
            @RequestHeader(value = "X-User", required = false) String user
    ) throws Exception {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to sign Comune transfer");
        }

        TransferManifest m = manifests.findById(manifestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "manifest not found"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        if ("SIGNED".equalsIgnoreCase(m.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "manifest already signed");
        }
        if (m.getItems() == null || m.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "manifest has no items");
        }

        // 2A) Upload signature file
        String key = "comune-transfers/" + manifestId + "/signature-" + UUID.randomUUID();
        try (var in = file.getInputStream()) {
            storage.put(key, in, file.getSize(), file.getContentType());
        }
        m.setSignatureKey(key);
        m.setSignedBy(signedBy != null && !signedBy.isBlank() ? signedBy : user);
        m.setSignedAt(OffsetDateTime.now());
        m.setStatus("SIGNED");

        // 2B) Update each item → TRANSFERRED_TO_COMUNE
        for (TransferManifestItem line : m.getItems()) {
            Item it = line.getItem();
            if (it == null) continue;
            String before = "{\"state\":\"" + it.getState() + "\"}";
            it.setState(Item.STATE_TRANSFERRED_TO_COMUNE);
            String after = "{\"state\":\"" + it.getState() + "\"}";
            audits.log(
                    "ITEM_TRANSFERRED_TO_COMUNE",
                    "ITEM",
                    it.getId(),
                    m.getSignedBy(),
                    "{\"before\":" + before + ",\"after\":" + after + "}"
            );
        }

        // 2C) Audit manifest sign
        audits.log(
                "COMUNE_TRANSFER_SIGNED",
                "TRANSFER_MANIFEST",
                m.getId(),
                m.getSignedBy(),
                "{\"signatureKey\":\"" + key + "\",\"itemCount\":" + m.getItems().size() + "}"
        );

        return ResponseEntity.ok(toDto(m));
    }

    // --- 3) Get single manifest with lines (internal) ---

    @GetMapping("/{id}")
    public ResponseEntity<TransferManifestDto> getOne(
            @PathVariable("id") UUID manifestId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to view Comune transfers");
        }

        return manifests.findById(manifestId)
                .map(m -> ResponseEntity.ok(toDto(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    // --- 4) List manifests (internal) ---

    @GetMapping
    public List<TransferManifestDto> list(
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestParam(name = "status", required = false) String status,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to list Comune transfers");
        }

        List<TransferManifest> src;

        if (depotId != null && status != null && !status.isBlank()) {
            src = manifests.findByDepot_IdAndStatusOrderByPreparedAtDesc(depotId, status);
        } else if (depotId != null) {
            src = manifests.findByDepot_IdOrderByPreparedAtDesc(depotId);
        } else {
            src = manifests.findAll();
        }

        List<TransferManifestDto> out = new ArrayList<>();
        for (TransferManifest m : src) {
            out.add(toDto(m));
        }
        return out;
    }

    // --- Helper to build DTO with presigned URLs ---

    private TransferManifestDto toDto(TransferManifest m) {
        String signatureUrl = null;
        if (m.getSignatureKey() != null && !m.getSignatureKey().isBlank()) {
            try {
                signatureUrl = storage.presignGet(m.getSignatureKey(), Duration.ofHours(1));
            } catch (Exception ignored) {
                signatureUrl = null;
            }
        }

        List<TransferManifestItemDto> itemsDto = new ArrayList<>();
        if (m.getItems() != null) {
            for (TransferManifestItem line : m.getItems()) {
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

        return TransferManifestDto.from(m, signatureUrl, itemsDto);
    }
}
