package it.bz.sta.lf;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audits")
public class Audit {

    @Id
    private UUID id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;  // ITEM | CLAIM | HANDOVER

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private String action;      // e.g. ITEM_STORED, ITEM_HANDOVER

    private String actor;       // X-User, or null

    @Column(columnDefinition = "text")
    private String details;     // JSON or simple text

    @Column(name = "at")
    private OffsetDateTime at = OffsetDateTime.now();

    public Audit() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public OffsetDateTime getAt() { return at; }
    public void setAt(OffsetDateTime at) { this.at = at; }
}
