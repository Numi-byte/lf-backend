package it.bz.sta.lf;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyAccessService {

    public static final String DEFAULT_COMPANY = "sta";

    private static final List<String> COMPANIES = List.of(
            "steinertouring",
            "silbernagl",
            "trenitalia",
            "auto-rainer",
            "autorainer",
            "pizzinini",
            "kronplatz",
            "simobil",
            "taferner",
            "sasabz",
            "holzer",
            "ksm",
            "sad",
            "sta"
    ).stream().sorted(Comparator.comparingInt(String::length).reversed()).toList();

    public Optional<String> resolveCompany(String user) {
        if (user == null || user.isBlank()) {
            return Optional.empty();
        }

        String normalizedUser = user.trim().toLowerCase();
        return COMPANIES.stream()
                .filter(normalizedUser::contains)
                .findFirst();
    }

    public String requireCompany(String user) {
        return resolveCompany(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "user email is not mapped to a supported company"
                ));
    }

    public String normalizeCompany(String company) {
        if (company == null || company.isBlank()) {
            return DEFAULT_COMPANY;
        }
        return company.trim().toLowerCase();
    }

    public void assignDepotCompany(Depot depot, String user) {
        depot.setCompany(requireCompany(user));
    }

    public void assignItemCompanyFromUser(Item item, String user) {
        item.setCompany(requireCompany(user));
    }

    public void assignItemCompanyFromDepot(Item item, Depot depot) {
        item.setCompany(normalizeCompany(depot != null ? depot.getCompany() : null));
    }

    public boolean canAccess(String userCompany, String entityCompany) {
        return normalizeCompany(userCompany).equals(normalizeCompany(entityCompany));
    }

    public boolean canAccessDepot(String userCompany, Depot depot) {
        return depot != null && canAccess(userCompany, depot.getCompany());
    }

    public boolean canAccessLocation(String userCompany, Location location) {
        return location != null && canAccessDepot(userCompany, location.getDepot());
    }

    public boolean canAccessItem(String userCompany, Item item) {
        return item != null && canAccess(userCompany, itemCompany(item));
    }

    public String itemCompany(Item item) {
        if (item == null) {
            return DEFAULT_COMPANY;
        }
        if (item.getCompany() != null && !item.getCompany().isBlank()) {
            return normalizeCompany(item.getCompany());
        }
        if (item.getCurrentLocation() != null && item.getCurrentLocation().getDepot() != null) {
            return normalizeCompany(item.getCurrentLocation().getDepot().getCompany());
        }
        return DEFAULT_COMPANY;
    }

    public void ensureDepotAccess(String userCompany, Depot depot, String message) {
        if (!canAccessDepot(userCompany, depot)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    public void ensureLocationAccess(String userCompany, Location location, String message) {
        if (!canAccessLocation(userCompany, location)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    public void ensureItemAccess(String userCompany, Item item, String message) {
        if (!canAccessItem(userCompany, item)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    public void ensureClaimAccess(String userCompany, Claim claim, String message) {
        if (claim == null || claim.getItem() == null || !canAccessItem(userCompany, claim.getItem())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    public void ensureHandoverAccess(String userCompany, Handover handover, String message) {
        if (handover == null || handover.getItem() == null || !canAccessItem(userCompany, handover.getItem())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    public void ensureManifestAccess(String userCompany, TransferManifest manifest, String message) {
        if (manifest == null || manifest.getDepot() == null || !canAccessDepot(userCompany, manifest.getDepot())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }
}