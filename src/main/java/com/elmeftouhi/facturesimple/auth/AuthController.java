package com.elmeftouhi.facturesimple.auth;

import com.elmeftouhi.facturesimple.auth.dto.AuthResponse;
import com.elmeftouhi.facturesimple.auth.dto.CreateTenantRequest;
import com.elmeftouhi.facturesimple.auth.dto.JoinTenantRequest;
import com.elmeftouhi.facturesimple.auth.dto.LoginRequest;
import com.elmeftouhi.facturesimple.auth.dto.RegisterRequest;
import com.elmeftouhi.facturesimple.auth.dto.SwitchTenantRequest;
import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import jakarta.validation.Valid;
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

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
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
            @Valid @RequestBody JoinTenantRequest request
    ) {
        return authService.joinTenantForUser(principal.userId(), request.tenantId());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(authorizationHeader);
    }
}

