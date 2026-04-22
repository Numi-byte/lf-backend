package it.bz.sta.lf.auth;

import it.bz.sta.lf.CompanyAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class UserRoleResolver {

    private final CompanyAccessService companyAccessService;

    @Value("${auth.admin-emails:}")
    private String adminEmails;

    public UserRoleResolver(CompanyAccessService companyAccessService) {
        this.companyAccessService = companyAccessService;
    }

    public AppUserPrincipal fromJwt(Jwt jwt) {
        String tid = claimAsString(jwt, "tid");
        String oid = claimAsString(jwt, "oid");
        String email = firstNonBlank(
                claimAsString(jwt, "preferred_username"),
                claimAsString(jwt, "upn"),
                claimAsString(jwt, "email")
        );

        String role = resolveRole(jwt, email);
        String company = resolveCompany(email);
        String id = "aad:" + safe(tid) + ":" + safe(oid);
        return new AppUserPrincipal(id, email, role, company);
    }

    public AppUserPrincipal fromLegacyUser(String userHeader) {
        String email = userHeader == null ? null : userHeader.trim();
        String company = resolveCompany(email);
        return new AppUserPrincipal("legacy:" + safe(email), email, "customer", company);
    }

    private String resolveRole(Jwt jwt, String email) {
        if (hasAdminRoleClaim(jwt)) {
            return "admin";
        }

        if (email != null && !email.isBlank()) {
            List<String> adminList = Arrays.stream(adminEmails.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();
            if (adminList.contains(email.toLowerCase(Locale.ROOT))) {
                return "admin";
            }
        }

        return "customer";
    }

    private boolean hasAdminRoleClaim(Jwt jwt) {
        Object rolesObj = jwt.getClaims().get("roles");
        if (rolesObj instanceof List<?> list) {
            return list.stream().map(String::valueOf).anyMatch(this::isAdminRole);
        }
        String role = claimAsString(jwt, "role");
        return isAdminRole(role);
    }

    private boolean isAdminRole(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("admin");
    }

    private String resolveCompany(String email) {
        return companyAccessService.resolveCompany(email).orElse(CompanyAccessService.DEFAULT_COMPANY);
    }

    private static String claimAsString(Jwt jwt, String claim) {
        Object value = jwt.getClaims().get(claim);
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value;
    }
}