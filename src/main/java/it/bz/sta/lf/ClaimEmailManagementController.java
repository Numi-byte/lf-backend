package it.bz.sta.lf;

import it.bz.sta.lf.auth.AppUserPrincipal;
import it.bz.sta.lf.dto.ClaimEmailSettingDto;
import it.bz.sta.lf.dto.ClaimEmailSettingUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/admin/email-management", "/api/admin/email-management"})
public class ClaimEmailManagementController {

    private final ClaimEmailManagementService emailManagementService;
    private final CompanyAccessService companyAccessService;

    public ClaimEmailManagementController(
            ClaimEmailManagementService emailManagementService,
            CompanyAccessService companyAccessService
    ) {
        this.emailManagementService = emailManagementService;
        this.companyAccessService = companyAccessService;
    }

    @GetMapping("/settings")
    public List<ClaimEmailSettingDto> listSettings(
            @RequestHeader(value = "X-User", required = false) String user,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        if (tryRequireAdminUser(user, principal, companyAccessService).isEmpty()) {
            return List.of();
        }
        return emailManagementService.listSettings().stream()
                .map(ClaimEmailSettingDto::from)
                .toList();
    }

    @GetMapping("/settings/{company}")
    public ClaimEmailSettingDto getSetting(
            @PathVariable("company") String company,
            @RequestHeader(value = "X-User", required = false) String user,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        requireAdminUser(user, principal, companyAccessService);
        return ClaimEmailSettingDto.from(emailManagementService.getSetting(company));
    }

    @PutMapping("/settings/{company}")
    public ClaimEmailSettingDto updateSetting(
            @PathVariable("company") String company,
            @RequestBody ClaimEmailSettingUpdateRequest req,
            @RequestHeader(value = "X-User", required = false) String user,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        AppUserPrincipal admin = requireAdminUser(user, principal, companyAccessService);
        String updatedBy = admin.email() != null && !admin.email().isBlank() ? admin.email() : user;
        return ClaimEmailSettingDto.from(emailManagementService.updateSetting(company, req, updatedBy));
    }

    private static AppUserPrincipal requireAdminUser(
            String user,
            AppUserPrincipal principal,
            CompanyAccessService companyAccessService
    ) {
        return tryRequireAdminUser(user, principal, companyAccessService)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "STA admin access required"));
    }

    private static Optional<AppUserPrincipal> tryRequireAdminUser(
            String user,
            AppUserPrincipal principal,
            CompanyAccessService companyAccessService
    ) {
        if (principal == null) {
            if (user == null || user.isBlank()) {
                return Optional.empty();
            }
            Optional<String> company = companyAccessService.resolveCompany(user)
                    .map(companyAccessService::normalizeCompany);
            if (company.isEmpty() || !CompanyAccessService.DEFAULT_COMPANY.equals(company.get())) {
                return Optional.empty();
            }
            return Optional.of(new AppUserPrincipal("legacy:" + user.trim(), user.trim(), "admin", company.get()));
        }

        String normalizedPrincipalCompany = companyAccessService.normalizeCompany(principal.company());
        if (!CompanyAccessService.DEFAULT_COMPANY.equals(normalizedPrincipalCompany)) {
            return Optional.empty();
        }

        if (principal.isAdmin()) {
            return Optional.of(principal);
        }

        // The legacy X-User fallback is trusted by AppAuthenticationFilter in development/back-office
        // deployments, but UserRoleResolver marks those principals as customers unless auth.admin-emails
        // is configured. Preserve the original STA X-User compatibility for this admin-only screen.
        if (isLegacyPrincipal(principal) && user != null && !user.isBlank()) {
            Optional<String> userCompany = companyAccessService.resolveCompany(user)
                    .map(companyAccessService::normalizeCompany);
            if (userCompany.isPresent() && CompanyAccessService.DEFAULT_COMPANY.equals(userCompany.get())) {
                return Optional.of(new AppUserPrincipal(principal.id(), principal.email(), "admin", normalizedPrincipalCompany));
            }
        }

        return Optional.empty();
    }

    private static boolean isLegacyPrincipal(AppUserPrincipal principal) {
        return principal.id() != null && principal.id().startsWith("legacy:");
    }
}