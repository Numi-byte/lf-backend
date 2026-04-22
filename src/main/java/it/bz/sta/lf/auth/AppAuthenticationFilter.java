package it.bz.sta.lf.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class AppAuthenticationFilter extends OncePerRequestFilter {

    private final AppSessionService sessionService;
    private final UserRoleResolver roleResolver;

    @Value("${auth.session.cookie-name:LF_SESSION}")
    private String cookieName;

    @Value("${auth.accept-x-user-fallback:true}")
    private boolean acceptLegacyUserFallback;

    public AppAuthenticationFilter(AppSessionService sessionService, UserRoleResolver roleResolver) {
        this.sessionService = sessionService;
        this.roleResolver = roleResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AppUserPrincipal principal = resolvePrincipal(request);

        HttpServletRequest requestToUse = request;
        if (principal != null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    "N/A",
                    List.of(new SimpleGrantedAuthority(principal.isAdmin() ? "ROLE_ADMIN" : "ROLE_CUSTOMER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (request.getHeader("X-User") == null && principal.email() != null) {
                requestToUse = new HeaderOverrideRequest(request, Map.of("X-User", principal.email()));
            }
        }

        try {
            filterChain.doFilter(requestToUse, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private AppUserPrincipal resolvePrincipal(HttpServletRequest request) {
        String sessionToken = cookieValue(request, cookieName);
        Optional<AppSession> session = sessionService.resolve(sessionToken);
        if (session.isPresent()) {
            return session.get().user();
        }

        if (acceptLegacyUserFallback) {
            String user = request.getHeader("X-User");
            if (user != null && !user.isBlank()) {
                return roleResolver.fromLegacyUser(user);
            }
        }

        return null;
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    static class HeaderOverrideRequest extends HttpServletRequestWrapper {

        private final Map<String, String> overrideHeaders;

        HeaderOverrideRequest(HttpServletRequest request, Map<String, String> overrideHeaders) {
            super(request);
            this.overrideHeaders = overrideHeaders;
        }

        @Override
        public String getHeader(String name) {
            String value = overrideHeaders.get(name);
            return value != null ? value : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = overrideHeaders.get(name);
            if (value != null) {
                return Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(overrideHeaders.keySet());
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            return Collections.enumeration(names);
        }
    }
}