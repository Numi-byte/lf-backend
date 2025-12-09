package it.bz.sta.lf;

import it.bz.sta.lf.dto.ItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final AuditService audits;

    public ItemController(ItemRepository repo, LocationRepository locations, AuditService audits) {
        this.repo = repo;
        this.locations = locations;
        this.audits = audits;
    }

    // small request DTOs
    public record CreateItem(String description) {}
    public record StoreReq(UUID locationId) {}

    // ---------- Basic list (internal, login required) ----------
    @GetMapping
    public List<ItemDto> all(
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to list items");
        }

        return repo.findAll().stream().map(ItemDto::from).toList();
    }

    // ---------- Get single item by id (internal, login required) ----------
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getOne(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to view items");
        }

        return repo.findById(id)
                .map(item -> ResponseEntity.ok(ItemDto.from(item)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------- Create → REPORTED (internal, login required) ----------
    @PostMapping
    public ResponseEntity<ItemDto> create(
            @RequestBody CreateItem req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to create items");
        }

        if (req == null || req.description() == null || req.description().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }

        Item item = new Item(UUID.randomUUID(), req.description(), OffsetDateTime.now());
        // state defaults to REPORTED in entity
        Item saved = repo.save(item);

        audits.log(
                "ITEM_REPORTED",
                "ITEM",
                saved.getId(),
                user,
                "{\"description\":\"" + saved.getDescription() + "\"}"
        );

        return ResponseEntity.ok(ItemDto.from(saved));
    }

    // ---------- Store → SHELVED (internal, login required) ----------
    @PostMapping("/{id}/store")
    public ResponseEntity<ItemDto> store(
            @PathVariable("id") UUID id,
            @RequestBody StoreReq req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to store items");
        }

        if (req == null || req.locationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId is required");
        }

        Item it = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        Location loc = locations.findById(req.locationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "location not found"));

        String before = "{\"state\":\"" + it.getState() + "\",\"currentLocationId\":\"" +
                (it.getCurrentLocation() == null ? "" : it.getCurrentLocation().getId()) + "\"}";

        it.setCurrentLocation(loc);
        it.setState(Item.STATE_SHELVED);

        String after = "{\"state\":\"" + it.getState() + "\",\"currentLocationId\":\"" + loc.getId() + "\"}";

        audits.log(
                "ITEM_SHELVED",
                "ITEM",
                it.getId(),
                user,
                "{\"before\":" + before + ",\"after\":" + after + "}"
        );

        return ResponseEntity.ok(ItemDto.from(it));
    }

    // ---------- Mark READY_FOR_TRANSFER (internal, login required) ----------
    @PostMapping("/{id}/ready-for-transfer")
    public ResponseEntity<ItemDto> readyForTransfer(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to mark items READY_FOR_TRANSFER");
        }

        Item it = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        // Only allow from SHELVED or ON_HOLD
        if (!Item.STATE_SHELVED.equals(it.getState()) && !Item.STATE_ON_HOLD.equals(it.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "item must be SHELVED or ON_HOLD to mark READY_FOR_TRANSFER");
        }

        String before = "{\"state\":\"" + it.getState() + "\"}";
        it.setState(Item.STATE_READY_FOR_TRANSFER);
        String after = "{\"state\":\"" + it.getState() + "\"}";

        audits.log(
                "ITEM_READY_FOR_TRANSFER",
                "ITEM",
                it.getId(),
                user,
                "{\"before\":" + before + ",\"after\":" + after + "}"
        );

        return ResponseEntity.ok(ItemDto.from(it));
    }

    // ---------- General search (internal, login required) ----------
    @GetMapping("/search")
    public List<ItemDto> search(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to search items");
        }

        OffsetDateTime fromTs = null, toTs = null;
        try { if (from != null && !from.isBlank()) fromTs = OffsetDateTime.parse(from); } catch (Exception ignored) {}
        try { if (to   != null && !to.isBlank())   toTs   = OffsetDateTime.parse(to);   } catch (Exception ignored) {}

        // 1) DB: filter by state + depot
        List<Item> items = repo.search(state, depotId);

        // 2) Java: filter by date range
        if (fromTs != null) {
            OffsetDateTime finalFromTs = fromTs;
            items = items.stream()
                    .filter(i -> i.getFoundAt() != null && !i.getFoundAt().isBefore(finalFromTs))
                    .toList();
        }

        if (toTs != null) {
            OffsetDateTime finalToTs = toTs;
            items = items.stream()
                    .filter(i -> i.getFoundAt() != null && !i.getFoundAt().isAfter(finalToTs))
                    .toList();
        }

        // 3) Java: free-text search on description
        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            items = items.stream()
                    .filter(i -> i.getDescription() != null &&
                            i.getDescription().toLowerCase().contains(needle))
                    .toList();
        }

        return items.stream().map(ItemDto::from).toList();
    }

    // ---------- Archive search for CS (RETURNED + TRANSFERRED_TO_COMUNE) ----------
    @GetMapping("/archive")
    public List<ItemDto> archive(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required to search archive");
        }

        OffsetDateTime fromTs = null, toTs = null;
        try { if (from != null && !from.isBlank()) fromTs = OffsetDateTime.parse(from); } catch (Exception ignored) {}
        try { if (to   != null && !to.isBlank())   toTs   = OffsetDateTime.parse(to);   } catch (Exception ignored) {}

        // 1) DB: only archive states + depot
        List<Item> items = repo.searchArchive(depotId);

        // 2) Java: date filter
        if (fromTs != null) {
            OffsetDateTime finalFromTs = fromTs;
            items = items.stream()
                    .filter(i -> i.getFoundAt() != null && !i.getFoundAt().isBefore(finalFromTs))
                    .toList();
        }

        if (toTs != null) {
            OffsetDateTime finalToTs = toTs;
            items = items.stream()
                    .filter(i -> i.getFoundAt() != null && !i.getFoundAt().isAfter(finalToTs))
                    .toList();
        }

        // 3) Java: text filter
        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            items = items.stream()
                    .filter(i -> i.getDescription() != null &&
                            i.getDescription().toLowerCase().contains(needle))
                    .toList();
        }

        return items.stream().map(ItemDto::from).toList();
    }
}
