package it.bz.sta.lf.dto;

import java.util.List;

public record ClaimEmailSettingUpdateRequest(
        String recipients,
        String claimantSubjectTemplate,
        String claimantBodyTemplate,
        String companySubjectTemplate,
        String companyBodyTemplate,
        String claimantUpdateSubjectTemplate,
        String claimantUpdateBodyTemplate,
        String companyUpdateSubjectTemplate,
        String companyUpdateBodyTemplate,
        List<EmailSettingGroupUpdateRequest> groups
) {
    public record EmailSettingGroupUpdateRequest(
            String key,
            String claimantSubjectTemplate,
            String claimantBodyTemplate,
            String companySubjectTemplate,
            String companyBodyTemplate
    ) {}
}