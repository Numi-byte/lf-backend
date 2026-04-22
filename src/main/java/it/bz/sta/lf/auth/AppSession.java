package it.bz.sta.lf.auth;

import java.time.Instant;

public record AppSession(
        String token,
        AppUserPrincipal user,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return expiresAt == null || !expiresAt.isAfter(now);
    }
}