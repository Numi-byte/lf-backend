package it.bz.sta.lf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "claim_email_settings")
public class ClaimEmailSetting {

    @Id
    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String recipients;

    @Column(name = "claimant_subject_template", nullable = false)
    private String claimantSubjectTemplate;

    @Column(name = "claimant_body_template", nullable = false)
    private String claimantBodyTemplate;

    @Column(name = "company_subject_template", nullable = false)
    private String companySubjectTemplate;

    @Column(name = "company_body_template", nullable = false)
    private String companyBodyTemplate;

    @Column(name = "claimant_update_subject_template", nullable = false)
    private String claimantUpdateSubjectTemplate;

    @Column(name = "claimant_update_body_template", nullable = false)
    private String claimantUpdateBodyTemplate;

    @Column(name = "company_update_subject_template", nullable = false)
    private String companyUpdateSubjectTemplate;

    @Column(name = "company_update_body_template", nullable = false)
    private String companyUpdateBodyTemplate;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "updated_by")
    private String updatedBy;

    public ClaimEmailSetting() {}

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }

    public String getClaimantSubjectTemplate() { return claimantSubjectTemplate; }
    public void setClaimantSubjectTemplate(String claimantSubjectTemplate) { this.claimantSubjectTemplate = claimantSubjectTemplate; }

    public String getClaimantBodyTemplate() { return claimantBodyTemplate; }
    public void setClaimantBodyTemplate(String claimantBodyTemplate) { this.claimantBodyTemplate = claimantBodyTemplate; }

    public String getCompanySubjectTemplate() { return companySubjectTemplate; }
    public void setCompanySubjectTemplate(String companySubjectTemplate) { this.companySubjectTemplate = companySubjectTemplate; }

    public String getCompanyBodyTemplate() { return companyBodyTemplate; }
    public void setCompanyBodyTemplate(String companyBodyTemplate) { this.companyBodyTemplate = companyBodyTemplate; }

    public String getClaimantUpdateSubjectTemplate() { return claimantUpdateSubjectTemplate; }
    public void setClaimantUpdateSubjectTemplate(String claimantUpdateSubjectTemplate) { this.claimantUpdateSubjectTemplate = claimantUpdateSubjectTemplate; }

    public String getClaimantUpdateBodyTemplate() { return claimantUpdateBodyTemplate; }
    public void setClaimantUpdateBodyTemplate(String claimantUpdateBodyTemplate) { this.claimantUpdateBodyTemplate = claimantUpdateBodyTemplate; }

    public String getCompanyUpdateSubjectTemplate() { return companyUpdateSubjectTemplate; }
    public void setCompanyUpdateSubjectTemplate(String companyUpdateSubjectTemplate) { this.companyUpdateSubjectTemplate = companyUpdateSubjectTemplate; }

    public String getCompanyUpdateBodyTemplate() { return companyUpdateBodyTemplate; }
    public void setCompanyUpdateBodyTemplate(String companyUpdateBodyTemplate) { this.companyUpdateBodyTemplate = companyUpdateBodyTemplate; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}