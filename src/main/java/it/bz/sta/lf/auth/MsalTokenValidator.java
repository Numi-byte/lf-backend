package it.bz.sta.lf.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class MsalTokenValidator {

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, JwtDecoder> decoderByTenant = new ConcurrentHashMap<>();

    @Value("${auth.msal.allowed-client-id:}")
    private String allowedClientId;

    @Value("${auth.msal.allowed-tenants:common}")
    private String allowedTenants;

    public MsalTokenValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Jwt validate(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "missing token");
        }

        Map<String, Object> payload = decodePayload(token);
        String tid = asString(payload.get("tid"));
        if (tid == null || tid.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "token missing tenant id");
        }
        enforceTenantAllowed(tid);

        Jwt jwt;
        try {
            jwt = decoderForTenant(tid).decode(token);
        } catch (JwtException ex) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid token", ex);
        }

        if (allowedClientId != null && !allowedClientId.isBlank()) {
            String aud = jwt.getAudience().isEmpty() ? null : jwt.getAudience().getFirst();
            if (!allowedClientId.equals(aud)) {
                throw new ResponseStatusException(UNAUTHORIZED, "invalid audience");
            }
        }

        return jwt;
    }

    private void enforceTenantAllowed(String tid) {
        if (allowedTenants == null || allowedTenants.isBlank() || "common".equalsIgnoreCase(allowedTenants)) {
            return;
        }

        List<String> tenants = List.of(allowedTenants.split(","));
        boolean allowed = tenants.stream().map(String::trim).filter(s -> !s.isBlank()).anyMatch(tid::equalsIgnoreCase);
        if (!allowed) {
            throw new ResponseStatusException(UNAUTHORIZED, "tenant not allowed");
        }
    }

    private JwtDecoder decoderForTenant(String tid) {
        return decoderByTenant.computeIfAbsent(tid, key -> {
            String jwkSetUri = "https://login.microsoftonline.com/" + key + "/discovery/v2.0/keys";
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer("https://login.microsoftonline.com/" + key + "/v2.0");
            decoder.setJwtValidator(withIssuer);
            return decoder;
        });
    }

    private Map<String, Object> decodePayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new ResponseStatusException(UNAUTHORIZED, "malformed token");
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(decoded, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new ResponseStatusException(UNAUTHORIZED, "malformed token", ex);
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}