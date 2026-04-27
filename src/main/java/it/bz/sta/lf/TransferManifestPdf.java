package it.bz.sta.lf;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transfer_manifest_pdfs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transfer_manifest_pdfs_manifest_lang", columnNames = {"manifest_id", "lang"})
        }
)
public class TransferManifestPdf {

    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id", nullable = false)
    private TransferManifest manifest;

    @Column(name = "lang", nullable = false, length = 8)
    private String lang;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "pdf_data", nullable = false, columnDefinition = "bytea")
    private byte[] pdfData;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "generated_by")
    private String generatedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TransferManifest getManifest() {
        return manifest;
    }

    public void setManifest(TransferManifest manifest) {
        this.manifest = manifest;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public void setPdfData(byte[] pdfData) {
        this.pdfData = pdfData;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(OffsetDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }
}