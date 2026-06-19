package com.elmeftouhi.facturesimple.auth;

import com.elmeftouhi.facturesimple.auth.dto.AuthResponse;
import com.elmeftouhi.facturesimple.auth.dto.LoginRequest;
import com.elmeftouhi.facturesimple.auth.dto.RegisterRequest;
import com.elmeftouhi.facturesimple.security.AppUserDetails;
import com.elmeftouhi.facturesimple.security.JwtProperties;
import com.elmeftouhi.facturesimple.security.JwtService;
import com.elmeftouhi.facturesimple.security.TokenBlacklistService;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.facturesimple.shared.exception.TenantAccessDeniedException;
import com.elmeftouhi.facturesimple.tenant.Tenant;
import com.elmeftouhi.facturesimple.tenant.TenantRepository;
import com.elmeftouhi.facturesimple.user.AppUser;
import com.elmeftouhi.facturesimple.user.AppUserRepository;
import com.elmeftouhi.facturesimple.user.Role;
import com.elmeftouhi.facturesimple.user.UserTenant;
import com.elmeftouhi.facturesimple.user.UserTenantRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already exists");
        }

        Tenant tenant = createTenantOrThrow(request.tenantName());

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.getRoles().add(Role.USER);
        user.setDefaultTenant(tenant);
        user = appUserRepository.save(user);

        UserTenant userTenant = new UserTenant();
        userTenant.setUser(user);
        userTenant.setTenant(tenant);
        userTenantRepository.save(userTenant);

        return buildAuthResponse(user, tenant.getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        AppUser user = appUserRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user, user.getDefaultTenant().getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse switchTenant(Long userId, Long tenantId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userTenantRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new TenantAccessDeniedException("You do not belong to this tenant");
        }

        return buildAuthResponse(user, tenantId);
    }

    @Transactional
    public AuthResponse createTenantForUser(Long userId, String tenantName) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tenant tenant = createTenantOrThrow(tenantName);

        UserTenant userTenant = new UserTenant();
        userTenant.setUser(user);
        userTenant.setTenant(tenant);
        userTenantRepository.save(userTenant);

        return buildAuthResponse(user, tenant.getId());
    }

    @Transactional
    public AuthResponse joinTenantForUser(Long userId, Long tenantId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (userTenantRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new ConflictException("You already belong to this tenant");
        }

        UserTenant userTenant = new UserTenant();
        userTenant.setUser(user);
        userTenant.setTenant(tenant);
        userTenantRepository.save(userTenant);

        return buildAuthResponse(user, tenantId);
    }

    @Transactional(readOnly = true)
    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        tokenBlacklistService.revoke(token, jwtService.extractExpiration(token));
    }

    private AuthResponse buildAuthResponse(AppUser user, Long selectedTenantId) {
        Set<Long> allowedTenantIds = userTenantRepository.findAllByUserId(user.getId())
                .stream()
                .map(userTenant -> userTenant.getTenant().getId())
                .collect(Collectors.toCollection(HashSet::new));
        allowedTenantIds.add(user.getDefaultTenant().getId());

        String token = jwtService.generateToken(user, selectedTenantId, allowedTenantIds);
        return new AuthResponse(token, "Bearer", jwtProperties.getExpirationMinutes(), selectedTenantId, allowedTenantIds);
    }

    private Tenant createTenantOrThrow(String tenantName) {
        String normalizedTenantName = tenantName.trim();
        if (tenantRepository.existsByNameIgnoreCase(normalizedTenantName)) {
            throw new ConflictException("Tenant name already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(normalizedTenantName);
        return tenantRepository.save(tenant);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new BadRequestException("Missing bearer token");
        }
        return token;
    }
}

