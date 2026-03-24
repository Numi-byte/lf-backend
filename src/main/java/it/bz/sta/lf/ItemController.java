package it.bz.sta.lf;

import it.bz.sta.lf.catalog.CategoryCatalog;
import it.bz.sta.lf.dto.ItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository repo;
    private final LocationRepository locations;
    private final DepotRepository depots;
    private final AuditService audits;
    private final CategoryCatalog categoryCatalog;
    private final CompanyAccessService companyAccess;

    public ItemController(
            ItemRepository repo,
            LocationRepository locations,
            DepotRepository depots,
            AuditService audits,
            CategoryCatalog categoryCatalog,
            CompanyAccessService companyAccess
    ) {
        this.repo = repo;
        this.locations = locations;
        this.depots = depots;
        this.audits = audits;
        this.categoryCatalog = categoryCatalog;
        this.companyAccess = companyAccess;
    }

    public record CreateItem(
            String description,
            String categoryMain,
            String categorySub,
            String transportType,
            String transportLine,
            String transportLineDe
    ) {}

    public record UpdateItem(
            String description,
            String categoryMain,
            String categorySub,
            String transportType,
            String transportLine,
            String transportLineDe
    ) {}

    public record StoreReq(UUID locationId) {}

    public record TransferToComuneReq(UUID comuneTransferId) {}

    @GetMapping
    public List<ItemDto> all(
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to list items");
        String company = companyAccess.requireCompany(user);
        return repo.findAll().stream()
                .filter(item -> companyAccess.canAccessItem(company, item))
                .map(ItemDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getOne(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to view items");
        String company = companyAccess.requireCompany(user);

        return repo.findById(id)
                .map(item -> {
                    companyAccess.ensureItemAccess(company, item, "item not found");
                    return ResponseEntity.ok(ItemDto.from(item));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ItemDto> create(
            @RequestBody CreateItem req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to create items");

        if (req == null || req.description() == null || req.description().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }

        boolean mainBlank = (req.categoryMain() == null || req.categoryMain().isBlank());
        boolean subBlank = (req.categorySub() == null || req.categorySub().isBlank());

        String main;
        String sub;

        if (mainBlank && subBlank) {
            main = "MISC";
            sub = "OTHER";
        } else if (mainBlank || subBlank) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryMain and categorySub must be provided together");
        } else {
            CategoryCatalog.Canonical canon = categoryCatalog.canonicalize(req.categoryMain(), req.categorySub());
            main = canon.main();
            sub = canon.sub();

            if (!categoryCatalog.isValidMain(main)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid categoryMain: " + main);
            }
            if (!categoryCatalog.isValidSub(main, sub)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid categorySub for " + main + ": " + sub);
            }
        }

        Item item = new Item(UUID.randomUUID(), req.description(), OffsetDateTime.now());
        companyAccess.assignItemCompanyFromUser(item, user);
        item.setCategoryMain(main);
        item.setCategorySub(sub);
        item.setTransportType(blankToNull(req.transportType()));
        item.setTransportLine(blankToNull(req.transportLine()));
        item.setTransportLineDe(resolveGermanLine(req.transportLine(), req.transportLineDe()));

        Item saved = repo.save(item);

        audits.log(
                "ITEM_REPORTED",
                "ITEM",
                saved.getId(),
                user,
                "{"
                        + "\"description\":\"" + saved.getDescription() + "\","
                        + "\"company\":\"" + saved.getCompany() + "\","
                        + "\"categoryMain\":\"" + saved.getCategoryMain() + "\","
                        + "\"categorySub\":\"" + saved.getCategorySub() + "\","
                        + "\"transportType\":" + jsonNullable(saved.getTransportType()) + ","
                        + "\"transportLine\":" + jsonNullable(saved.getTransportLine()) + ","
                        + "\"transportLineDe\":" + jsonNullable(saved.getTransportLineDe())
                        + "}"
        );

        return ResponseEntity.ok(ItemDto.from(saved));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<ItemDto> update(
            @PathVariable("id") UUID id,
            @RequestBody UpdateItem req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to update items");
        String company = companyAccess.requireCompany(user);
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        Item item = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        companyAccess.ensureItemAccess(company, item, "item not found");

        if (req.description() != null && !req.description().isBlank()) {
            item.setDescription(req.description().trim());
        }

        boolean mainProvided = req.categoryMain() != null;
        boolean subProvided = req.categorySub() != null;
        if (mainProvided || subProvided) {
            if (!mainProvided || !subProvided || req.categoryMain().isBlank() || req.categorySub().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryMain and categorySub must be provided together");
            }

            CategoryCatalog.Canonical canon = categoryCatalog.canonicalize(req.categoryMain(), req.categorySub());
            String main = canon.main();
            String sub = canon.sub();

            if (!categoryCatalog.isValidMain(main)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid categoryMain: " + main);
            }
            if (!categoryCatalog.isValidSub(main, sub)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid categorySub for " + main + ": " + sub);
            }

            item.setCategoryMain(main);
            item.setCategorySub(sub);
        }

        if (req.transportType() != null) item.setTransportType(blankToNull(req.transportType()));
        if (req.transportLine() != null) item.setTransportLine(blankToNull(req.transportLine()));
        if (req.transportLineDe() != null || req.transportLine() != null) {
            item.setTransportLineDe(resolveGermanLine(req.transportLine() != null ? req.transportLine() : item.getTransportLine(), req.transportLineDe()));
        }

        return ResponseEntity.ok(ItemDto.from(item));
    }


    @PostMapping("/{id}/store")
    @Transactional
    public ResponseEntity<ItemDto> store(
            @PathVariable("id") UUID id,
            @RequestBody StoreReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to store items");
        String company = companyAccess.requireCompany(user);

        if (req == null || req.locationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId is required");
        }

        Item item = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        Location location = locations.findById(req.locationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "location not found"));

        String before = "{\"state\":\"" + item.getState() + "\",\"currentLocationId\":\"" +
                (item.getCurrentLocation() == null ? "" : item.getCurrentLocation().getId()) + "\",\"company\":\"" + item.getCompany() + "\"}";

        item.setCurrentLocation(location);
        companyAccess.assignItemCompanyFromDepot(item, location.getDepot());
        item.setState(Item.STATE_SHELVED);

        String after = "{\"state\":\"" + item.getState() + "\",\"currentLocationId\":\"" + location.getId() + "\",\"company\":\"" + item.getCompany() + "\"}";

        audits.log(
                "ITEM_SHELVED",
                "ITEM",
                item.getId(),
                user,
                "{\"before\":" + before + ",\"after\":" + after + "}"
        );

        return ResponseEntity.ok(ItemDto.from(item));
    }


    @PostMapping("/{id}/ready-for-transfer")
    @Transactional
    public ResponseEntity<ItemDto> readyForTransfer(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to mark items READY_FOR_TRANSFER");
        String company = companyAccess.requireCompany(user);

        Item item = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        companyAccess.ensureItemAccess(company, item, "item not found");

        if (!Item.STATE_SHELVED.equals(item.getState()) && !Item.STATE_ON_HOLD.equals(item.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item must be SHELVED or ON_HOLD to mark READY_FOR_TRANSFER");
        }

        String before = "{\"state\":\"" + item.getState() + "\"}";
        item.setState(Item.STATE_READY_FOR_TRANSFER);
        String after = "{\"state\":\"" + item.getState() + "\"}";

        audits.log("ITEM_READY_FOR_TRANSFER", "ITEM", item.getId(), user, "{\"before\":" + before + ",\"after\":" + after + "}");

        return ResponseEntity.ok(ItemDto.from(item));
    }


    @PostMapping("/{id}/transfer-to-comune")
    @Transactional
    public ResponseEntity<ItemDto> transferToComune(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) TransferToComuneReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to transfer items to comune");
        String company = companyAccess.requireCompany(user);

        Item item = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        companyAccess.ensureItemAccess(company, item, "item not found");

        if (!Item.STATE_READY_FOR_TRANSFER.equals(item.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item must be READY_FOR_TRANSFER to transfer to comune");
        }

        String before = "{\"state\":\"" + item.getState() + "\"}";
        item.setState(Item.STATE_TRANSFERRED_TO_COMUNE);
        String after = "{\"state\":\"" + item.getState() + "\"}";

        String comuneTransferId = (req != null && req.comuneTransferId() != null) ? req.comuneTransferId().toString() : null;

        audits.log(
                "ITEM_TRANSFERRED_TO_COMUNE",
                "ITEM",
                item.getId(),
                user,
                "{"
                        + "\"before\":" + before + ","
                        + "\"after\":" + after + ","
                        + (comuneTransferId == null ? "\"comuneTransferId\":null" : "\"comuneTransferId\":\"" + comuneTransferId + "\"")
                        + "}"
        );

        return ResponseEntity.ok(ItemDto.from(item));
    }


    @GetMapping("/search")
    public List<ItemDto> search(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to search items");
        String company = companyAccess.requireCompany(user);
        validateDepotAccess(depotId, company);

        OffsetDateTime fromTs = null, toTs = null;
        try { if (from != null && !from.isBlank()) fromTs = OffsetDateTime.parse(from); } catch (Exception ignored) {}
        try { if (to   != null && !to.isBlank())   toTs   = OffsetDateTime.parse(to);   } catch (Exception ignored) {}

        List<Item> items = repo.search(state, depotId).stream()
                .filter(item -> companyAccess.canAccessItem(company, item))
                .toList();

        // 2) Java: filter by date range
        if (fromTs != null) {
            OffsetDateTime finalFromTs = fromTs;
            items = items.stream().filter(i -> i.getFoundAt() != null && !i.getFoundAt().isBefore(finalFromTs)).toList();
        }
        if (toTs != null) {
            OffsetDateTime finalToTs = toTs;
            items = items.stream().filter(i -> i.getFoundAt() != null && !i.getFoundAt().isAfter(finalToTs)).toList();
        }


        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            items = items.stream().filter(i -> i.getDescription() != null && i.getDescription().toLowerCase().contains(needle)).toList();
        }

        return items.stream().map(ItemDto::from).toList();
    }


    @GetMapping("/archive")
    public List<ItemDto> archive(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user, "login required to search archive");
        String company = companyAccess.requireCompany(user);
        validateDepotAccess(depotId, company);

        OffsetDateTime fromTs = null, toTs = null;
        try { if (from != null && !from.isBlank()) fromTs = OffsetDateTime.parse(from); } catch (Exception ignored) {}
        try { if (to   != null && !to.isBlank())   toTs   = OffsetDateTime.parse(to);   } catch (Exception ignored) {}

        // 1) DB: only archive states + depot
        List<Item> items = repo.searchArchive(depotId).stream()
                .filter(item -> companyAccess.canAccessItem(company, item))
                .toList();


        if (fromTs != null) {
            OffsetDateTime finalFromTs = fromTs;
            items = items.stream().filter(i -> i.getFoundAt() != null && !i.getFoundAt().isBefore(finalFromTs)).toList();
        }

        if (toTs != null) {
            OffsetDateTime finalToTs = toTs;
            items = items.stream().filter(i -> i.getFoundAt() != null && !i.getFoundAt().isAfter(finalToTs)).toList();
        }


        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            items = items.stream().filter(i -> i.getDescription() != null && i.getDescription().toLowerCase().contains(needle))
                    .toList();
        }

        return items.stream().map(ItemDto::from).toList();
    }

    private void validateDepotAccess(UUID depotId, String company) {
        if (depotId == null) {
            return;
        }
        Depot depot = depots.findById(depotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));
        companyAccess.ensureDepotAccess(company, depot, "depot not found");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String resolveGermanLine(String line, String lineDe) {
        if (lineDe != null && !lineDe.isBlank()) {
            return lineDe.trim();
        }
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] split = line.split("/", 2);
        return split.length > 1 ? split[0].trim() : null;
    }

    private static String jsonNullable(String value) {
        return value == null ? "null" : "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private static void requireUser(String user, String msg) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
        }
    }
}
