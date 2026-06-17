package it.bz.sta.lf.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_email_otps")
public class CustomerEmailOtpEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "device_hash", nullable = false)
    private String deviceHash;

    @Column(name = "attempts_remaining", nullable = false)
    private int attemptsRemaining;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public String getDeviceHash() { return deviceHash; }
    public void setDeviceHash(String deviceHash) { this.deviceHash = deviceHash; }
    public int getAttemptsRemaining() { return attemptsRemaining; }
    public void setAttemptsRemaining(int attemptsRemaining) { this.attemptsRemaining = attemptsRemaining; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}