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
            String main = normalizeCode(rule.getMainCode());
            if (main == null) {
                continue;
            }

            String sub = normalizeCode(rule.getSubCode());
            if (sub == null) {
                hiddenMains.add(main);
            } else {
                hiddenSubsByMain.computeIfAbsent(main, ignored -> new HashSet<>())
                        .add(sub);
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
            String main = normalizeCode(rule.mainCode());
            if (main == null) {
                continue;
            }
            String sub = normalizeCode(rule.subCode());

            String key = main + "|" + (sub == null ? "" : sub);
            CatalogVisibilityRule entity = new CatalogVisibilityRule();
            entity.setMainCode(main);
            entity.setSubCode(sub);
            byKey.put(key, entity);
        }

        List<CatalogVisibilityRule> newRules = byKey.values().stream().toList();

        repository.saveAll(newRules);
        return getVisibilityState();
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
