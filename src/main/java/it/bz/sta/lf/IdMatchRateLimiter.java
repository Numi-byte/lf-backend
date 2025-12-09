package it.bz.sta.lf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for ID-match endpoint.
 *
 * Key = X-User (public portal user id / session id).
 * Window sliding logic:
 *  - For each key we track a windowStart + count.
 *  - If now is outside window -> reset windowStart + count=1.
 *  - If inside window and count > maxRequests -> 429 Too Many Requests.
 *
 * This is good enough for internal usage & low traffic.
 */
@Component
public class IdMatchRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(IdMatchRateLimiter.class);

    private final int maxRequests;
    private final Duration window;

    private static class Counter {
        int count;
        Instant windowStart;
    }

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public IdMatchRateLimiter(
            @Value("${rateLimit.idMatch.maxRequests:10}") int maxRequests,
            @Value("${rateLimit.idMatch.windowSeconds:600}") long windowSeconds
    ) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
        log.info("IdMatchRateLimiter initialized: maxRequests={}, windowSeconds={}", maxRequests, windowSeconds);
    }

    /**
     * Throws ResponseStatusException(429) if limit is exceeded.
     */
    public void checkAllowed(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            // Controller already enforces X-User, but double-check for safety.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        }

        Instant now = Instant.now();

        Counter counter = counters.compute(userKey, (key, existing) -> {
            if (existing == null) {
                Counter c = new Counter();
                c.count = 1;
                c.windowStart = now;
                return c;
            }

            // Still inside the current window?
            if (existing.windowStart.plus(window).isAfter(now)) {
                existing.count++;
                return existing;
            } else {
                // Window expired -> reset
                existing.windowStart = now;
                existing.count = 1;
                return existing;
            }
        });

        if (counter.count > maxRequests) {
            log.warn("ID-MATCH rate limit exceeded for user={} (count={} in {} seconds)",
                    userKey, counter.count, window.getSeconds());
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "too many ID-match attempts, please try again later"
            );
        }
    }
}
