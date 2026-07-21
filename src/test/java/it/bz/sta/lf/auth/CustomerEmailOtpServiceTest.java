package it.bz.sta.lf.auth;

import it.bz.sta.lf.ClaimEmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

class CustomerEmailOtpServiceTest {

    private CustomerEmailOtpRepository otps;
    private ClaimEmailNotificationService mailService;
    private CustomerEmailOtpService service;

    @BeforeEach
    void setUp() {
        otps = mock(CustomerEmailOtpRepository.class);
        mailService = mock(ClaimEmailNotificationService.class);
        service = new CustomerEmailOtpService(otps, mailService);
        ReflectionTestUtils.setField(service, "otpTtlMinutes", 5L);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "rateLimitWindowMinutes", 60L);
        ReflectionTestUtils.setField(service, "maxRequestsPerWindow", 5L);
        ReflectionTestUtils.setField(service, "subject", "Your lost-and-found sign-in code");
    }

    @Test
    void requestOtpReturnsServiceUnavailableWhenMailDeliveryFails() {
        when(otps.countByEmailAndCreatedAtAfter(eq("customer@example.com"), any())).thenReturn(0L);
        doThrow(new MailException("no sender") {})
                .when(mailService)
                .sendAuthenticationEmail(eq("customer@example.com"), eq("Your lost-and-found sign-in code"), any());

        assertThatThrownBy(() -> service.requestOtp("Customer@Example.com", "device-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);
                    assertThat(responseStatusException.getReason()).isEqualTo("OTP email delivery is temporarily unavailable");
                });

        verify(otps).save(any(CustomerEmailOtpEntity.class));
    }
}
