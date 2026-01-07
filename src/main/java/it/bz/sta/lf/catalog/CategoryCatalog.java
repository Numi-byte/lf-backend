package it.bz.sta.lf.catalog;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Locale;

@Component
public class CategoryCatalog {

    private final CategoryCatalogController controller;

    // main -> allowed sub codes
    private Map<String, Set<String>> allowedSubsByMain = Map.of();

    // main -> (aliasSub -> canonicalSub)
    private Map<String, Map<String, String>> subAliasesByMain = Map.of();

    public record Canonical(String main, String sub) {}

    public CategoryCatalog(CategoryCatalogController controller) {
        this.controller = controller;
    }

    @PostConstruct
    void init() {
        // Build allow-list from your existing CategoryCatalogController (single source of truth)
        Map<String, Set<String>> allowed = new HashMap<>();
        for (CategoryCatalogController.Category c : controller.categories()) {
            String main = norm(c.code());
            Set<String> subs = new HashSet<>();
            if (c.subcategories() != null) {
                for (CategoryCatalogController.SubCategory sc : c.subcategories()) {
                    subs.add(norm(sc.code()));
                }
            }
            allowed.put(main, Collections.unmodifiableSet(subs));
        }
        allowedSubsByMain = Collections.unmodifiableMap(allowed);

        // Aliases for old client codes / human variations
        // IMPORTANT: Your KEYS catalog uses VEHICLE_KEY_SINGLE, not SINGLE_VEHICLE_KEY
        Map<String, Map<String, String>> aliases = new HashMap<>();

        aliases.put("KEYS", Map.of(
                "SINGLE_VEHICLE_KEY", "VEHICLE_KEY_SINGLE",
                "SINGLE_KEY", "KEY_SINGLE",
                "KEY_RING", "KEYCHAIN",
                "BUNCH_WITH_VEHICLE", "KEY_BUNCH_WITH_VEHICLE",
                "BUNCH_NO_VEHICLE", "KEY_BUNCH_NO_VEHICLE",
                "ACCESS_CARD", "ACCESS_CARD_CHIP",
                "GATE_REMOTE", "GATE_GARAGE_REMOTE"
        ));

        aliases.put("ID_DOCS", Map.of(
                "DOCUMENT", "DOCUMENT_GENERAL",
                "VACCINATION_PASSPORT", "VACCINATION_CERT"
        ));

        aliases.put("WALLETS_MONEY", Map.of(
                "WALLET", "WALLET_PURSE",
                "CASH", "CASH_CURRENCY",
                "EVENT_TICKET", "EVENT_TICKET_VOUCHER"
        ));

        subAliasesByMain = Collections.unmodifiableMap(aliases);
    }

    private static String norm(String s) {
        return (s == null) ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    public Canonical canonicalize(String mainRaw, String subRaw) {
        String main = norm(mainRaw);
        String sub = norm(subRaw);

        if (main == null || sub == null) return new Canonical(main, sub);

        Map<String, String> aliases = subAliasesByMain.get(main);
        if (aliases != null) {
            sub = aliases.getOrDefault(sub, sub);
        }
        return new Canonical(main, sub);
    }

    public boolean isValidMain(String mainRaw) {
        String main = norm(mainRaw);
        return main != null && allowedSubsByMain.containsKey(main);
    }

    public boolean isValidSub(String mainRaw, String subRaw) {
        String main = norm(mainRaw);
        String sub = norm(subRaw);
        if (main == null || sub == null) return false;

        Set<String> allowed = allowedSubsByMain.get(main);
        return allowed != null && allowed.contains(sub);
    }
}
