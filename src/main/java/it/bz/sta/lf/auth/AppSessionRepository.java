package it.bz.sta.lf.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface AppSessionRepository extends JpaRepository<AppSessionEntity, String> {
    void deleteByExpiresAtBefore(Instant cutoff);
}