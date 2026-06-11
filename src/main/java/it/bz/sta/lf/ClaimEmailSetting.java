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

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}