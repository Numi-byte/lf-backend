package it.bz.sta.lf.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private CustomerEmailOtpService customerEmailOtpService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        customerEmailOtpService = mock(CustomerEmailOtpService.class);
        controller = new AuthController(
                mock(MsalTokenValidator.class),
                mock(UserRoleResolver.class),
                mock(AppSessionService.class),
                customerEmailOtpService
        );
        ReflectionTestUtils.setField(controller, "otpDeviceCookieName", "LF_OTP_DEVICE");
        ReflectionTestUtils.setField(controller, "customerOtpTtlMinutes", 5L);
        ReflectionTestUtils.setField(controller, "sameSite", "Lax");
        ReflectionTestUtils.setField(controller, "secureCookie", false);
    }

    @Test
    void requestCustomerOtpFromQueryAcceptsGetRequests() {
        when(customerEmailOtpService.newDeviceToken()).thenReturn("new-device-token");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        var result = controller.requestCustomerOtpFromQuery("customer@example.com", request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(customerEmailOtpService).requestOtp("customer@example.com", "new-device-token");
        verify(response).addHeader(eq("Set-Cookie"), contains("LF_OTP_DEVICE=new-device-token"));
        verify(response).addHeader(eq("Set-Cookie"), contains("Max-Age=300"));
    }

    @Test
    void requestCustomerOtpFromQueryReusesExistingDeviceCookie() {
        Cookie deviceCookie = mock(Cookie.class);
        when(deviceCookie.getName()).thenReturn("LF_OTP_DEVICE");
        when(deviceCookie.getValue()).thenReturn("existing-device-token");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{deviceCookie});
        HttpServletResponse response = mock(HttpServletResponse.class);

        controller.requestCustomerOtpFromQuery("customer@example.com", request, response);

        verify(customerEmailOtpService).requestOtp("customer@example.com", "existing-device-token");
    }

    @Test
    void requestCustomerOtpFromQueryRequiresEmail() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> controller.requestCustomerOtpFromQuery(null, request, response))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(customerEmailOtpService);
    }
}