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

@RestController
@RequestMapping("/admin/email-management")
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
        requireAdminUser(user, principal, companyAccessService);
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
        if (principal == null) {
            if (user == null || user.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
            }
            String company = companyAccessService.requireCompany(user);
            if (!CompanyAccessService.DEFAULT_COMPANY.equals(company)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "STA admin access required");
            }
            return new AppUserPrincipal("legacy:" + user.trim(), user.trim(), "admin", company);
        }

        if (!principal.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin access required");
        }
        if (!CompanyAccessService.DEFAULT_COMPANY.equals(companyAccessService.normalizeCompany(principal.company()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "STA admin access required");
        }
        return principal;
    }
}