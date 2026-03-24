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
    private final DepotRepository depots;
    private final CompanyAccessService companyAccess;

    public MemberItemController(ItemRepository items, DepotRepository depots, CompanyAccessService companyAccess) {
        this.items = items;
        this.depots = depots;
        this.companyAccess = companyAccess;
    }

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
        requireUser(user);
        String company = companyAccess.requireCompany(user);
        validateDepotAccess(depotId, company);

        OffsetDateTime fromTs = null, toTs = null;
        try { if (from != null && !from.isBlank()) fromTs = OffsetDateTime.parse(from); } catch (Exception ignored) {}
        try { if (to   != null && !to.isBlank())   toTs   = OffsetDateTime.parse(to);   } catch (Exception ignored) {}

        List<Item> list = items.search(null, depotId).stream()
                .filter(item -> companyAccess.canAccessItem(company, item))
                .filter(item -> !Item.STATE_RETURNED.equals(item.getState())
                        && !Item.STATE_TRANSFERRED_TO_COMUNE.equals(item.getState()))
                .toList();

        if (fromTs != null) {
            OffsetDateTime f = fromTs;
            list = list.stream().filter(i -> i.getFoundAt() != null && !i.getFoundAt().isBefore(f)).toList();
        }
        if (toTs != null) {
            OffsetDateTime t = toTs;
            list = list.stream().filter(i -> i.getFoundAt() != null && !i.getFoundAt().isAfter(t)).toList();
        }
        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            list = list.stream().filter(i -> i.getDescription() != null && i.getDescription().toLowerCase().contains(needle)).toList();
        }
        if (categoryMain != null && !categoryMain.isBlank()) {
            String cm = categoryMain.trim();
            list = list.stream().filter(i -> cm.equalsIgnoreCase(i.getCategoryMain())).toList();
        }
        if (categorySub != null && !categorySub.isBlank()) {
            String cs = categorySub.trim();
            list = list.stream().filter(i -> cs.equalsIgnoreCase(i.getCategorySub())).toList();
        }

        return list.stream().map(PublicItemDto::fromMember).toList();
    }

    @GetMapping("/{id}")
    public PublicItemDto getOne(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user);
        String company = companyAccess.requireCompany(user);

        Item item = items.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        companyAccess.ensureItemAccess(company, item, "item not found");

        if (Item.STATE_RETURNED.equals(item.getState()) || Item.STATE_TRANSFERRED_TO_COMUNE.equals(item.getState())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found");
        }

        return PublicItemDto.fromMember(item);
    }

    private void validateDepotAccess(UUID depotId, String company) {
        if (depotId == null) {
            return;
        }
        Depot depot = depots.findById(depotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "depot not found"));
        companyAccess.ensureDepotAccess(company, depot, "depot not found");
    }

    private static void requireUser(String user) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        }
    }
}
