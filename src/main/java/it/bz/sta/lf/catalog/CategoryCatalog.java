package it.bz.sta.lf.catalog;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CategoryCatalog {

    private final CategoryCatalogController controller;

    // main -> (aliasSub -> canonicalSub)
    private final Map<String, Map<String, String>> subAliasesByMain;

    public record Canonical(String main, String sub) {}

    public CategoryCatalog(CategoryCatalogController controller) {
        this.controller = controller;
        this.subAliasesByMain = buildAliases();
    }

    private static Map<String, Map<String, String>> buildAliases() {
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

        return Collections.unmodifiableMap(aliases);
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
        if (main == null) {
            return false;
        }

        return controller.categories().stream()
                .anyMatch(category -> main.equals(norm(category.code())));
    }

    public boolean isValidSub(String mainRaw, String subRaw) {
        String main = norm(mainRaw);
        String sub = norm(subRaw);
        if (main == null || sub == null) return false;

        return controller.categories().stream()
                .filter(category -> main.equals(norm(category.code())))
                .flatMap(category -> category.subcategories().stream())
                .anyMatch(subCategory -> sub.equals(norm(subCategory.code())));
    }
}
