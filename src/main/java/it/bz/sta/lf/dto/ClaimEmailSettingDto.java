package it.bz.sta.lf.dto;

import it.bz.sta.lf.ClaimEmailSetting;

import java.time.OffsetDateTime;

public record ClaimEmailSettingDto(
        String company,
        String recipients,
        String claimantSubjectTemplate,
        String claimantBodyTemplate,
        String companySubjectTemplate,
        String companyBodyTemplate,
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
                setting.getUpdatedAt(),
                setting.getUpdatedBy()
        );
    }
}