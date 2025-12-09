package it.bz.sta.lf;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class Claim {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "passenger_name")
    private String passengerName;

    @Column(name = "passenger_email")
    private String passengerEmail;

    @Column(name = "passenger_phone")
    private String passengerPhone;

    private String narrative;

    @Column(nullable = false)
    private String status = "new"; // new, in_review, approved, rejected, closed

    private String method; // pickup | ship

    @Column(name = "fee_cents")
    private Integer feeCents = 0;

    @Column(length = 3)
    private String currency = "EUR";

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // NEW: who created the claim in the public portal
    @Column(name = "public_user_id")
    private String publicUserId;

    // NEW: short reference code (e.g. shown in emails/letters)
    @Column(name = "public_reference_code")
    private String publicReferenceCode;

    public Claim() {}

    // getters/setters …

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }

    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Integer getFeeCents() { return feeCents; }
    public void setFeeCents(Integer feeCents) { this.feeCents = feeCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPublicUserId() { return publicUserId; }
    public void setPublicUserId(String publicUserId) { this.publicUserId = publicUserId; }

    public String getPublicReferenceCode() { return publicReferenceCode; }
    public void setPublicReferenceCode(String publicReferenceCode) { this.publicReferenceCode = publicReferenceCode; }
}
