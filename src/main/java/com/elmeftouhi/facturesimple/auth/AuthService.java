package com.elmeftouhi.facturesimple.auth;

import com.elmeftouhi.facturesimple.auth.dto.AuthResponse;
import com.elmeftouhi.facturesimple.auth.dto.LoginRequest;
import com.elmeftouhi.facturesimple.auth.dto.RegisterRequest;
import com.elmeftouhi.facturesimple.auth.dto.TenantInviteResponse;
import com.elmeftouhi.facturesimple.security.AppUserDetails;
import com.elmeftouhi.facturesimple.security.JwtProperties;
import com.elmeftouhi.facturesimple.security.JwtService;
import com.elmeftouhi.facturesimple.security.TokenBlacklistService;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.facturesimple.shared.exception.TenantAccessDeniedException;
import com.elmeftouhi.facturesimple.tenant.Tenant;
import com.elmeftouhi.facturesimple.tenant.TenantInvite;
import com.elmeftouhi.facturesimple.tenant.TenantInviteRepository;
import com.elmeftouhi.facturesimple.tenant.TenantRepository;
import com.elmeftouhi.facturesimple.user.AppUser;
import com.elmeftouhi.facturesimple.user.AppUserRepository;
import com.elmeftouhi.facturesimple.user.Role;
import com.elmeftouhi.facturesimple.user.UserTenant;
import com.elmeftouhi.facturesimple.user.UserTenantRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 10;
    private static final SecureRandom INVITE_RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final TenantInviteRepository tenantInviteRepository;
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
    public AuthResponse joinTenantForUser(Long userId, String inviteCode) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TenantInvite invite = tenantInviteRepository.findByCodeIgnoreCase(inviteCode.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired invite code"));

        Long tenantId = invite.getTenant().getId();
        Tenant tenant = invite.getTenant();

        if (userTenantRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new ConflictException("You already belong to this tenant");
        }

        Instant now = Instant.now();
        int consumed = tenantInviteRepository.consumeInviteIfActive(invite.getId(), now, now);
        if (consumed == 0) {
            throw new BadRequestException("Invalid or expired invite code");
        }

        UserTenant userTenant = new UserTenant();
        userTenant.setUser(user);
        userTenant.setTenant(tenant);
        try {
            userTenantRepository.save(userTenant);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("You already belong to this tenant");
        }

        return buildAuthResponse(user, tenantId);
    }

    @Transactional
    public TenantInviteResponse createTenantInviteForUser(Long userId, Long tenantId, Integer expiresInHours) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        boolean isGlobalAdmin = user.getRoles().contains(Role.ADMIN);
        boolean isTenantOwner = user.getDefaultTenant().getId().equals(tenantId);
        if (!isGlobalAdmin && !isTenantOwner) {
            throw new TenantAccessDeniedException("Only tenant owner or admin can create invites");
        }

        Instant expiresAt = Instant.now().plusSeconds(expiresInHours.longValue() * 3600);

        TenantInvite invite = new TenantInvite();
        invite.setCode(generateUniqueInviteCode());
        invite.setTenant(tenant);
        invite.setCreatedBy(user);
        invite.setExpiresAt(expiresAt);
        invite = tenantInviteRepository.save(invite);

        return new TenantInviteResponse(invite.getCode(), tenantId, invite.getExpiresAt());
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

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder codeBuilder = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                int index = INVITE_RANDOM.nextInt(INVITE_CODE_ALPHABET.length());
                codeBuilder.append(INVITE_CODE_ALPHABET.charAt(index));
            }
            String code = codeBuilder.toString();
            if (!tenantInviteRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique invite code");
    }
}

