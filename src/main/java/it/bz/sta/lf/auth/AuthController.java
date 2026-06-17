package it.bz.sta.lf.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    public record ExchangeRequest(String idToken, String accessToken) {}
    public record UserDto(String id, String email, String role, String company) {}
    public record ExchangeResponse(UserDto user) {}
    public record SessionResponse(boolean authenticated, UserDto user) {}
    public record OtpRequest(String email) {}
    public record OtpVerifyRequest(String email, String code) {}

    private final MsalTokenValidator tokenValidator;
    private final UserRoleResolver roleResolver;
    private final AppSessionService sessionService;
    private final CustomerEmailOtpService customerEmailOtpService;

    @Value("${auth.session.cookie-name:LF_SESSION}")
    private String cookieName;

    @Value("${auth.session.cookie-secure:true}")
    private boolean secureCookie;

    @Value("${auth.session.cookie-same-site:Lax}")
    private String sameSite;

    @Value("${auth.customer-otp.session-ttl-minutes:60}")
    private long customerOtpSessionTtlMinutes;

    @Value("${auth.customer-otp.device-cookie-name:LF_OTP_DEVICE}")
    private String otpDeviceCookieName;

    @Value("${auth.customer-otp.ttl-minutes:5}")
    private long customerOtpTtlMinutes;

    public AuthController(
            MsalTokenValidator tokenValidator,
            UserRoleResolver roleResolver,
            AppSessionService sessionService,
            CustomerEmailOtpService customerEmailOtpService
    ) {
        this.tokenValidator = tokenValidator;
        this.roleResolver = roleResolver;
        this.sessionService = sessionService;
        this.customerEmailOtpService = customerEmailOtpService;
    }

    @PostMapping("/msal/exchange")
    public ResponseEntity<ExchangeResponse> exchange(
            @RequestBody ExchangeRequest request,
            HttpServletResponse response
    ) {
        if (request == null || bothBlank(request.idToken(), request.accessToken())) {
            throw new ResponseStatusException(BAD_REQUEST, "idToken or accessToken is required");
        }

        String token = request.accessToken() != null && !request.accessToken().isBlank()
                ? request.accessToken()
                : request.idToken();

        Jwt jwt = tokenValidator.validate(token);
        AppUserPrincipal user = roleResolver.fromJwt(jwt);
        AppSession session = sessionService.create(user);
        writeSessionCookie(response, session.token(), 60 * 60 * 8);

        return ResponseEntity.ok(new ExchangeResponse(toUserDto(user)));
    }


    @PostMapping("/customer/otp/request")
    public ResponseEntity<Void> requestCustomerOtp(
            @RequestBody OtpRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "email is required");
        }
        String deviceToken = cookieValue(servletRequest, otpDeviceCookieName);
        if (deviceToken == null || deviceToken.isBlank()) {
            deviceToken = customerEmailOtpService.newDeviceToken();
        }
        customerEmailOtpService.requestOtp(request.email(), deviceToken);
        int deviceCookieMaxAge = Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(1, customerOtpTtlMinutes) * 60));
        writeCookie(response, otpDeviceCookieName, deviceToken, deviceCookieMaxAge);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/customer/otp/verify")
    public ResponseEntity<ExchangeResponse> verifyCustomerOtp(
            @RequestBody OtpVerifyRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "email and code are required");
        }
        String deviceToken = cookieValue(servletRequest, otpDeviceCookieName);
        AppUserPrincipal user = customerEmailOtpService.verifyOtp(request.email(), request.code(), deviceToken);
        long ttlMinutes = Math.max(1, customerOtpSessionTtlMinutes);
        AppSession session = sessionService.create(user, Duration.ofMinutes(ttlMinutes));
        writeSessionCookie(response, session.token(), Math.toIntExact(Math.min(Integer.MAX_VALUE, ttlMinutes * 60)));
        writeCookie(response, otpDeviceCookieName, "", 0);
        return ResponseEntity.ok(new ExchangeResponse(toUserDto(user)));
    }

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session(HttpServletRequest request) {
        String token = cookieValue(request, cookieName);
        return sessionService.resolve(token)
                .map(appSession -> ResponseEntity.ok(new SessionResponse(true, toUserDto(appSession.user()))))
                .orElseGet(() -> ResponseEntity.ok(new SessionResponse(false, null)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieValue(request, cookieName);
        sessionService.revoke(token);
        writeSessionCookie(response, "", 0);
        writeCookie(response, otpDeviceCookieName, "", 0);
        return ResponseEntity.noContent().build();
    }

    private void writeSessionCookie(HttpServletResponse response, String value, int maxAge) {
        writeCookie(response, cookieName, value, maxAge);
    }

    private void writeCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private UserDto toUserDto(AppUserPrincipal user) {
        return new UserDto(user.id(), user.email(), user.role(), user.company());
    }

    private boolean bothBlank(String idToken, String accessToken) {
        return (idToken == null || idToken.isBlank()) && (accessToken == null || accessToken.isBlank());
    }
}