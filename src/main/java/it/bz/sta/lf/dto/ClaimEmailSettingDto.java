package it.bz.sta.lf.dto;

import it.bz.sta.lf.ClaimEmailSetting;

import java.time.OffsetDateTime;
import java.util.List;

public record ClaimEmailSettingDto(
        String company,
        String recipients,
        String claimantSubjectTemplate,
        String claimantBodyTemplate,
        String companySubjectTemplate,
        String companyBodyTemplate,
        String claimantUpdateSubjectTemplate,
        String claimantUpdateBodyTemplate,
        String companyUpdateSubjectTemplate,
        String companyUpdateBodyTemplate,
        List<EmailSettingGroupDto> groups,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public static ClaimEmailSettingDto from(ClaimEmailSetting setting) {
        return new ClaimEmailSettingDto(
                setting.getCompany(),
                setting.getRecipients(),
                setting.getClaimantSubjectTemplate(),
                setting.getClaimantBodyTemplate(),
                setting.getCompanySubjectTemplate(),
                setting.getCompanyBodyTemplate(),
                setting.getClaimantUpdateSubjectTemplate(),
                setting.getClaimantUpdateBodyTemplate(),
                setting.getCompanyUpdateSubjectTemplate(),
                setting.getCompanyUpdateBodyTemplate(),
                List.of(
                        new EmailSettingGroupDto(
                                "confirmation-email",
                                "Confirmation Email",
                                setting.getClaimantSubjectTemplate(),
                                setting.getClaimantBodyTemplate(),
                                setting.getCompanySubjectTemplate(),
                                setting.getCompanyBodyTemplate()
                        ),
                        new EmailSettingGroupDto(
                                "update-email",
                                "Update Email",
                                setting.getClaimantUpdateSubjectTemplate(),
                                setting.getClaimantUpdateBodyTemplate(),
                                setting.getCompanyUpdateSubjectTemplate(),
                                setting.getCompanyUpdateBodyTemplate()
                        )
                ),
                setting.getUpdatedAt(),
                setting.getUpdatedBy()
        );
    }
    public record EmailSettingGroupDto(
            String key,
            String label,
            String claimantSubjectTemplate,
            String claimantBodyTemplate,
            String companySubjectTemplate,
            String companyBodyTemplate
    ) {}
}