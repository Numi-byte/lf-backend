package it.bz.sta.lf.dto;

public record ClaimEmailSettingUpdateRequest(
        String recipients,
        String claimantSubjectTemplate,
        String claimantBodyTemplate,
        String companySubjectTemplate,
        String companyBodyTemplate
) {}