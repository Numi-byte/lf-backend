package it.bz.sta.lf;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditRepository extends JpaRepository<Audit, UUID> {

    // optional, handy later if you want to show item history:
    List<Audit> findByEntityTypeAndEntityIdOrderByAtDesc(String entityType, UUID entityId);
}
