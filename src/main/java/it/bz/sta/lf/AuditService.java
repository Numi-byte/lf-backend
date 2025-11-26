package it.bz.sta.lf;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditRepository repo;

    public AuditService(AuditRepository repo) {
        this.repo = repo;
    }

    /**
     * Simple helper to write one audit row.
     *
     * @param action     e.g. "ITEM_HANDOVER"
     * @param entityType e.g. "ITEM", "HANDOVER", "CLAIM"
     * @param entityId   UUID of the entity
     * @param actor      user name from X-User, can be null
     * @param details    JSON or plain text, can be null
     */
    public void log(String action, String entityType, UUID entityId, String actor, String details) {
        Audit a = new Audit();
        a.setId(UUID.randomUUID());
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setActor(actor);
        a.setDetails(details);
        repo.save(a);
    }
}
