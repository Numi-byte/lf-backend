package it.bz.sta.lf;

import it.bz.sta.lf.dto.PublicItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/member/items")
public class MemberItemController {

    private final ItemRepository items;

    public MemberItemController(ItemRepository items) {
        this.items = items;
    }

    /**
     * Member search (login required):
     * - Allows depotId filtering (unlike anonymous public search)
     * - Excludes archived items by default
     * - Supports category filters for icon-based UI (categoryMain/categorySub)
     */
    @GetMapping("/search")
    public List<PublicItemDto> search(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "depotId", required = false) UUID depotId,
            @RequestParam(name = "categoryMain", required = false) String categoryMain,
            @RequestParam(name = "categorySub", required = false) String categorySub,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        }

        OffsetDateTime fromTs = null, toTs = null;
        try { if (from != null && !from.isBlank()) fromTs = OffsetDateTime.parse(from); } catch (Exception ignored) {}
        try { if (to   != null && !to.isBlank())   toTs   = OffsetDateTime.parse(to);   } catch (Exception ignored) {}

        // DB search: allow depotId filtering here (member endpoint)
        List<Item> list = items.search(null, depotId);

        // Exclude archived
        list = list.stream()
                .filter(i -> !Item.STATE_RETURNED.equals(i.getState())
                        && !Item.STATE_TRANSFERRED_TO_COMUNE.equals(i.getState()))
                .toList();

        // Date filters
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

        // Text filter
        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            list = list.stream()
                    .filter(i -> i.getDescription() != null
                            && i.getDescription().toLowerCase().contains(needle))
                    .toList();
        }

        // Category filters (important for your public UI icons)
        if (categoryMain != null && !categoryMain.isBlank()) {
            String cm = categoryMain.trim();
            list = list.stream()
                    .filter(i -> cm.equalsIgnoreCase(i.getCategoryMain()))
                    .toList();
        }

        if (categorySub != null && !categorySub.isBlank()) {
            String cs = categorySub.trim();
            list = list.stream()
                    .filter(i -> cs.equalsIgnoreCase(i.getCategorySub()))
                    .toList();
        }

        return list.stream().map(PublicItemDto::fromMember).toList();
    }

    /**
     * Member get-one (login required, still hides archived items).
     */
    @GetMapping("/{id}")
    public PublicItemDto getOne(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        }

        Item item = items.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));

        if (Item.STATE_RETURNED.equals(item.getState()) ||
                Item.STATE_TRANSFERRED_TO_COMUNE.equals(item.getState())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found");
        }

        return PublicItemDto.fromMember(item);
    }
}
