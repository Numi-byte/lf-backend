package it.bz.sta.lf.catalog;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CatalogVisibilityService {

    public record VisibilityState(Set<String> hiddenMainCodes, Map<String, Set<String>> hiddenSubCodesByMain) {
        boolean isMainHidden(String mainCode) {
            return hiddenMainCodes.contains(mainCode);
        }

        boolean isSubHidden(String mainCode, String subCode) {
            return hiddenSubCodesByMain.getOrDefault(mainCode, Set.of()).contains(subCode);
        }
    }

    public record UpsertRule(String mainCode, String subCode, boolean hidden) {
    }

    private final CatalogVisibilityRuleRepository repository;

    public CatalogVisibilityService(CatalogVisibilityRuleRepository repository) {
        this.repository = repository;
    }


    public VisibilityState getVisibilityState() {
        List<CatalogVisibilityRule> rules = repository.findAll();

        Set<String> hiddenMains = new HashSet<>();
        Map<String, Set<String>> hiddenSubsByMain = new HashMap<>();

        for (CatalogVisibilityRule rule : rules) {
            if (rule.getSubCode() == null || rule.getSubCode().isBlank()) {
                hiddenMains.add(rule.getMainCode());
            } else {
                hiddenSubsByMain.computeIfAbsent(rule.getMainCode(), ignored -> new HashSet<>())
                        .add(rule.getSubCode());
            }
        }

        return new VisibilityState(
                Collections.unmodifiableSet(hiddenMains),
                Collections.unmodifiableMap(hiddenSubsByMain)
        );
    }

    public VisibilityState replaceRules(List<UpsertRule> rules) {
        repository.deleteAllInBatch();

        Map<String, CatalogVisibilityRule> byKey = new LinkedHashMap<>();
        for (UpsertRule rule : rules) {
            if (!rule.hidden()) {
                continue;
            }
            String key = rule.mainCode() + "|" + (rule.subCode() == null ? "" : rule.subCode());
            CatalogVisibilityRule entity = new CatalogVisibilityRule();
            entity.setMainCode(rule.mainCode());
            entity.setSubCode(rule.subCode());
            byKey.put(key, entity);
        }

        List<CatalogVisibilityRule> newRules = byKey.values().stream().toList();

        repository.saveAll(newRules);
        return getVisibilityState();
    }
}
