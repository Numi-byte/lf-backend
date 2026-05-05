package it.bz.sta.lf.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class AppSessionService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final AppSessionRepository sessions;

    @Value("${auth.session.ttl-minutes:480}")
    private long sessionTtlMinutes;

    public AppSessionService(AppSessionRepository sessions) {
        this.sessions = sessions;
    }

    public AppSession create(AppUserPrincipal user) {
        String token = newToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(Math.max(5, sessionTtlMinutes)));

        AppSessionEntity entity = new AppSessionEntity();
        entity.setToken(token);
        entity.setUserId(user.id());
        entity.setEmail(user.email());
        entity.setRole(user.role());
        entity.setCompany(user.company());
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(now);
        sessions.save(entity);

        return new AppSession(token, user, expiresAt);
    }

    public Optional<AppSession> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<AppSessionEntity> entityOpt = sessions.findById(token);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        AppSessionEntity entity = entityOpt.get();
        Instant now = Instant.now();
        if (entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(now)) {
            sessions.deleteById(token);
            return Optional.empty();
        }

        AppUserPrincipal user = new AppUserPrincipal(
                entity.getUserId(),
                entity.getEmail(),
                entity.getRole(),
                entity.getCompany()
        );
        return Optional.of(new AppSession(entity.getToken(), user, entity.getExpiresAt()));
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            sessions.deleteById(token);
        }
    }

    @Scheduled(fixedDelayString = "${auth.session.cleanup-ms:300000}")
    @Transactional
    public void cleanupExpiredSessions() {
        sessions.deleteByExpiresAtBefore(Instant.now());
    }

    private String newToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}