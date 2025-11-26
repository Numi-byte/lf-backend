package it.bz.sta.lf;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "handovers")
public class Handover {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "depot_id")
    private UUID depotId; // snapshot of depot at handover time

    @Column(nullable = false)
    private String type = "PERSON"; // PERSON | COMUNE

    @Column(name = "performed_by")
    private String performedBy;

    // PERSON
    @Column(name = "person_name")
    private String personName;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "document_number")
    private String documentNumber;

    // COMUNE
    @Column(name = "comune_name")
    private String comuneName;

    @Column(name = "comune_reference")
    private String comuneReference;

    private String notes;

    // S3 keys for both sides of document
    @Column(name = "doc_front_key")
    private String docFrontKey;

    @Column(name = "doc_back_key")
    private String docBackKey;

    @Column(name = "attachment_key")
    private String attachmentKey;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Handover() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public UUID getDepotId() { return depotId; }
    public void setDepotId(UUID depotId) { this.depotId = depotId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getComuneName() { return comuneName; }
    public void setComuneName(String comuneName) { this.comuneName = comuneName; }

    public String getComuneReference() { return comuneReference; }
    public void setComuneReference(String comuneReference) { this.comuneReference = comuneReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDocFrontKey() { return docFrontKey; }
    public void setDocFrontKey(String docFrontKey) { this.docFrontKey = docFrontKey; }

    public String getDocBackKey() { return docBackKey; }
    public void setDocBackKey(String docBackKey) { this.docBackKey = docBackKey; }

    public String getAttachmentKey() { return attachmentKey; }
    public void setAttachmentKey(String attachmentKey) { this.attachmentKey = attachmentKey; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
