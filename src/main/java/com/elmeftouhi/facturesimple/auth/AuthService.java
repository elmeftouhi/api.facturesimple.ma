package com.elmeftouhi.facturesimple.auth;

import com.elmeftouhi.facturesimple.auth.dto.AuthResponse;
import com.elmeftouhi.facturesimple.auth.dto.LoginRequest;
import com.elmeftouhi.facturesimple.auth.dto.RegisterRequest;
import com.elmeftouhi.facturesimple.security.AppUserDetails;
import com.elmeftouhi.facturesimple.security.JwtProperties;
import com.elmeftouhi.facturesimple.security.JwtService;
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
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName().trim());
        tenant = tenantRepository.save(tenant);

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

        Set<Long> allowedTenantIds = Set.of(tenant.getId());
        String token = jwtService.generateToken(user, tenant.getId(), allowedTenantIds);
        return new AuthResponse(token, "Bearer", jwtProperties.getExpirationMinutes(), tenant.getId(), allowedTenantIds);
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

        Long selectedTenantId = user.getDefaultTenant().getId();
        Set<Long> allowedTenantIds = userTenantRepository.findAllByUserId(user.getId())
                .stream()
                .map(userTenant -> userTenant.getTenant().getId())
                .collect(Collectors.toCollection(HashSet::new));
        allowedTenantIds.add(selectedTenantId);

        String token = jwtService.generateToken(user, selectedTenantId, allowedTenantIds);
        return new AuthResponse(token, "Bearer", jwtProperties.getExpirationMinutes(), selectedTenantId, allowedTenantIds);
    }

    @Transactional(readOnly = true)
    public AuthResponse switchTenant(Long userId, Long tenantId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userTenantRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new TenantAccessDeniedException("You do not belong to this tenant");
        }

        Set<Long> allowedTenantIds = userTenantRepository.findAllByUserId(user.getId())
                .stream()
                .map(userTenant -> userTenant.getTenant().getId())
                .collect(Collectors.toCollection(HashSet::new));
        allowedTenantIds.add(user.getDefaultTenant().getId());

        String token = jwtService.generateToken(user, tenantId, allowedTenantIds);
        return new AuthResponse(token, "Bearer", jwtProperties.getExpirationMinutes(), tenantId, allowedTenantIds);
    }
}

