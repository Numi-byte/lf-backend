package it.bz.sta.lf.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AppSessionService {

    private final ConcurrentMap<String, AppSession> sessions = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.session.ttl-minutes:480}")
    private long sessionTtlMinutes;

    public AppSession create(AppUserPrincipal user) {
        String token = newToken();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(Math.max(5, sessionTtlMinutes)));
        AppSession session = new AppSession(token, user, expiresAt);
        sessions.put(token, session);
        return session;
    }

    public Optional<AppSession> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        AppSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }

        if (session.isExpired(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }

        return Optional.of(session);
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    private String newToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}