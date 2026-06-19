package com.elmeftouhi.facturesimple.auth;

import com.elmeftouhi.facturesimple.auth.dto.AuthResponse;
import com.elmeftouhi.facturesimple.auth.dto.CreateTenantRequest;
import com.elmeftouhi.facturesimple.auth.dto.CreateTenantInviteRequest;
import com.elmeftouhi.facturesimple.auth.dto.JoinTenantRequest;
import com.elmeftouhi.facturesimple.auth.dto.LoginRequest;
import com.elmeftouhi.facturesimple.auth.dto.RegisterRequest;
import com.elmeftouhi.facturesimple.auth.dto.SwitchTenantRequest;
import com.elmeftouhi.facturesimple.auth.dto.TenantInviteResponse;
import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import com.elmeftouhi.facturesimple.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
    private static final int LOGIN_MAX_REQUESTS = 5;
    private static final Duration JOIN_WINDOW = Duration.ofMinutes(1);
    private static final int JOIN_MAX_REQUESTS = 10;

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = resolveClientIp(servletRequest);
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        rateLimitService.assertWithinLimit(
                "login:" + clientIp + ":" + normalizedEmail,
                LOGIN_MAX_REQUESTS,
                LOGIN_WINDOW,
                "Too many login attempts. Please try again in a minute"
        );
        return authService.login(request);
    }

    @PostMapping("/switch-tenant")
    public AuthResponse switchTenant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody SwitchTenantRequest request
    ) {
        return authService.switchTenant(principal.userId(), request.tenantId());
    }

    @PostMapping("/tenants")
    public AuthResponse createTenant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateTenantRequest request
    ) {
        return authService.createTenantForUser(principal.userId(), request.tenantName());
    }

    @PostMapping("/tenants/join")
    public AuthResponse joinTenant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody JoinTenantRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = resolveClientIp(servletRequest);
        rateLimitService.assertWithinLimit(
                "join-tenant:" + principal.userId() + ":" + clientIp,
                JOIN_MAX_REQUESTS,
                JOIN_WINDOW,
                "Too many tenant join attempts. Please try again in a minute"
        );
        return authService.joinTenantForUser(principal.userId(), request.inviteCode());
    }

    @PostMapping("/tenants/invites")
    public TenantInviteResponse createTenantInvite(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateTenantInviteRequest request
    ) {
        return authService.createTenantInviteForUser(principal.userId(), request.tenantId(), request.expiresInHours());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(authorizationHeader);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

