package it.bz.sta.lf;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transfer_manifests")
public class TransferManifest {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @Column(name = "comune_name", nullable = false)
    private String comuneName;

    @Column(name = "comune_contact")
    private String comuneContact;

    @Column(name = "boxes_count")
    private Integer boxesCount;

    @Column(name = "seals_count")
    private String sealsCount;

    @Column(name = "prepared_by")
    private String preparedBy;

    @Column(name = "prepared_at")
    private OffsetDateTime preparedAt = OffsetDateTime.now();

    @Column(name = "signed_by")
    private String signedBy;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signature_key")
    private String signatureKey;

    @Column(nullable = false)
    private String status = "OPEN"; // OPEN | SIGNED

    @OneToMany(mappedBy = "manifest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransferManifestItem> items = new ArrayList<>();

    public TransferManifest() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    public String getComuneName() { return comuneName; }
    public void setComuneName(String comuneName) { this.comuneName = comuneName; }

    public String getComuneContact() { return comuneContact; }
    public void setComuneContact(String comuneContact) { this.comuneContact = comuneContact; }

    public Integer getBoxesCount() { return boxesCount; }
    public void setBoxesCount(Integer boxesCount) { this.boxesCount = boxesCount; }

    public String getSealsCount() { return sealsCount; }
    public void setSealsCount(String sealsCount) { this.sealsCount = sealsCount; }

    public String getPreparedBy() { return preparedBy; }
    public void setPreparedBy(String preparedBy) { this.preparedBy = preparedBy; }

    public OffsetDateTime getPreparedAt() { return preparedAt; }
    public void setPreparedAt(OffsetDateTime preparedAt) { this.preparedAt = preparedAt; }

    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }

    public OffsetDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(OffsetDateTime signedAt) { this.signedAt = signedAt; }

    public String getSignatureKey() { return signatureKey; }
    public void setSignatureKey(String signatureKey) { this.signatureKey = signatureKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<TransferManifestItem> getItems() { return items; }
    public void setItems(List<TransferManifestItem> items) { this.items = items; }
}
