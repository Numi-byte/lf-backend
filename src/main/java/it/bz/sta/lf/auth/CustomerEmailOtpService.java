package it.bz.sta.lf.auth;

import it.bz.sta.lf.ClaimEmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import static org.springframework.http.HttpStatus.*;

@Service
public class CustomerEmailOtpService {
    private static final Logger log = LoggerFactory.getLogger(CustomerEmailOtpService.class);

    private final SecureRandom secureRandom = new SecureRandom();
    private final CustomerEmailOtpRepository otps;
    private final ClaimEmailNotificationService mailService;

    @Value("${auth.customer-otp.ttl-minutes:5}")
    private long otpTtlMinutes;

    @Value("${auth.customer-otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${auth.customer-otp.rate-limit-window-minutes:60}")
    private long rateLimitWindowMinutes;

    @Value("${auth.customer-otp.max-requests-per-window:5}")
    private long maxRequestsPerWindow;

    @Value("${auth.customer-otp.subject:Your lost-and-found sign-in code}")
    private String subject;

    public CustomerEmailOtpService(CustomerEmailOtpRepository otps, ClaimEmailNotificationService mailService) {
        this.otps = otps;
        this.mailService = mailService;
    }

    @Transactional
    public void requestOtp(String email, String deviceToken) {
        String normalizedEmail = normalizeEmail(email);
        String deviceHash = deviceHash(deviceToken);
        Instant now = Instant.now();
        long recentRequests = otps.countByEmailAndCreatedAtAfter(
                normalizedEmail,
                now.minus(Duration.ofMinutes(Math.max(1, rateLimitWindowMinutes)))
        );
        if (recentRequests >= Math.max(1, maxRequestsPerWindow)) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Too many OTP requests. Please try again later.");
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        CustomerEmailOtpEntity otp = new CustomerEmailOtpEntity();
        otp.setEmail(normalizedEmail);
        otp.setCodeHash(hash(code));
        otp.setDeviceHash(deviceHash);
        otp.setAttemptsRemaining(Math.max(1, maxAttempts));
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plus(Duration.ofMinutes(Math.max(1, otpTtlMinutes))));
        otps.save(otp);

        try {
            mailService.sendAuthenticationEmail(
                    normalizedEmail,
                    subject,
                    "Your sign-in code is " + code + ". It expires in " + Math.max(1, otpTtlMinutes) + " minutes."
            );
        } catch (MailException e) {
            log.error("Could not send customer OTP email to={}", normalizedEmail, e);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "OTP email delivery is temporarily unavailable");
        }
    }

    @Transactional
    public AppUserPrincipal verifyOtp(String email, String code, String deviceToken) {
        String normalizedEmail = normalizeEmail(email);
        String deviceHash = deviceHash(deviceToken);
        if (code == null || !code.matches("\\d{6}")) {
            throw new ResponseStatusException(BAD_REQUEST, "A 6-digit OTP code is required");
        }

        CustomerEmailOtpEntity otp = otps
                .findFirstByEmailAndDeviceHashAndConsumedAtIsNullAndExpiresAtAfterAndAttemptsRemainingGreaterThanOrderByCreatedAtDesc(
                        normalizedEmail,
                        deviceHash,
                        Instant.now(),
                        0
                )
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "OTP is invalid or expired"));

        if (!MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.UTF_8), otp.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            otp.setAttemptsRemaining(Math.max(0, otp.getAttemptsRemaining() - 1));
            otps.save(otp);
            throw new ResponseStatusException(UNAUTHORIZED, "OTP is invalid");
        }

        otp.setConsumedAt(Instant.now());
        otps.save(otp);
        return new AppUserPrincipal("customer:" + normalizedEmail, normalizedEmail, "customer", "customer");
    }

    @Scheduled(fixedDelayString = "${auth.customer-otp.cleanup-ms:300000}")
    @Transactional
    public void cleanupExpiredOtps() {
        otps.deleteExpiredOrConsumed(Instant.now());
    }

    public String newDeviceToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String deviceHash(String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "OTP must be verified on the same device that requested it");
        }
        return hash(deviceToken);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ResponseStatusException(BAD_REQUEST, "A valid email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash OTP", e);
        }
    }
}
