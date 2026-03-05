package it.bz.sta.lf.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/catalog/admin")
public class CatalogAdminController {

    private final CategoryCatalogController categoryCatalogController;
    private final CatalogVisibilityService visibilityService;

    public CatalogAdminController(
            CategoryCatalogController categoryCatalogController,
            CatalogVisibilityService visibilityService
    ) {
        this.categoryCatalogController = categoryCatalogController;
        this.visibilityService = visibilityService;
    }

    public record VisibilityRuleRequest(String mainCode, String subCode, boolean hidden) {}

    public record VisibilityRuleResponse(String mainCode, String subCode) {}

    public record VisibilityResponse(
            List<String> hiddenMainCodes,
            List<VisibilityRuleResponse> hiddenSubCategories
    ) {}

    @GetMapping("/visibility")
    public VisibilityResponse getVisibility(
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user);
        return toResponse(visibilityService.getVisibilityState());
    }

    @PutMapping("/visibility")
    public VisibilityResponse replaceVisibility(
            @RequestBody List<VisibilityRuleRequest> req,
            @RequestHeader(value = "X-User", required = false) String user
    ) {
        requireUser(user);

        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        Map<String, Set<String>> allowedSubsByMain = loadAllowedSubsByMain();

        List<CatalogVisibilityService.UpsertRule> upsertRules = req.stream()
                .map(rule -> validateAndNormalize(rule, allowedSubsByMain))
                .toList();

        CatalogVisibilityService.VisibilityState visibilityState = visibilityService.replaceRules(upsertRules);
        return toResponse(visibilityState);
    }

    private static void requireUser(String user) {
        if (user == null || user.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        }
    }

    private Map<String, Set<String>> loadAllowedSubsByMain() {
        Map<String, Set<String>> map = new HashMap<>();
        for (CategoryCatalogController.Category category : categoryCatalogController.rawCategories()) {
            Set<String> subs = new HashSet<>();
            for (CategoryCatalogController.SubCategory subCategory : category.subcategories()) {
                subs.add(subCategory.code());
            }
            map.put(category.code(), Collections.unmodifiableSet(subs));
        }
        return Collections.unmodifiableMap(map);
    }

    private CatalogVisibilityService.UpsertRule validateAndNormalize(
            VisibilityRuleRequest rule,
            Map<String, Set<String>> allowedSubsByMain
    ) {
        if (rule == null || rule.mainCode() == null || rule.mainCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mainCode is required");
        }

        String main = rule.mainCode().trim().toUpperCase(Locale.ROOT);
        String sub = (rule.subCode() == null || rule.subCode().isBlank())
                ? null
                : rule.subCode().trim().toUpperCase(Locale.ROOT);

        Set<String> allowedSubs = allowedSubsByMain.get(main);
        if (allowedSubs == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid mainCode: " + main);
        }

        if (sub != null && !allowedSubs.contains(sub)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid subCode for " + main + ": " + sub);
        }

        return new CatalogVisibilityService.UpsertRule(main, sub, rule.hidden());
    }

    private VisibilityResponse toResponse(CatalogVisibilityService.VisibilityState state) {
        List<String> hiddenMainCodes = state.hiddenMainCodes().stream().sorted().toList();
        List<VisibilityRuleResponse> hiddenSubCategories = state.hiddenSubCodesByMain().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().stream()
                        .sorted()
                        .map(subCode -> new VisibilityRuleResponse(entry.getKey(), subCode)))
                .toList();

        return new VisibilityResponse(hiddenMainCodes, hiddenSubCategories);
    }
}
