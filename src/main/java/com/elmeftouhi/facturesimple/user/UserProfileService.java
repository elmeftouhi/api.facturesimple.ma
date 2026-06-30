package com.elmeftouhi.facturesimple.user;

import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.facturesimple.tenant.dto.TenantMembershipResponse;
import com.elmeftouhi.facturesimple.user.dto.MeResponse;
import com.elmeftouhi.facturesimple.user.dto.UserProfileUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AppUserRepository appUserRepository;
    private final UserTenantRepository userTenantRepository;

    @Transactional(readOnly = true)
    public MeResponse me(JwtPrincipal principal) {
        AppUser user = appUserRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String displayedName = buildDisplayedName(user);

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                displayedName,
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getDefaultTenant().getId(),
                principal.selectedTenantId(),
                user.getRoles()
        );
    }

    private String buildDisplayedName(AppUser user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        } else if (firstName != null && !firstName.isBlank()) {
            return firstName;
        } else if (lastName != null && !lastName.isBlank()) {
            return lastName;
        } else {
            return user.getEmail();
        }
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipResponse> myTenants(JwtPrincipal principal) {
        AppUser user = appUserRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long defaultTenantId = user.getDefaultTenant().getId();
        return userTenantRepository.findAllByUserId(principal.userId())
                .stream()
                .map(userTenant -> new TenantMembershipResponse(
                        userTenant.getTenant().getId(),
                        userTenant.getTenant().getName(),
                        userTenant.getTenant().getId().equals(defaultTenantId)
                ))
                .toList();
    }

    @Transactional
    public MeResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim().isEmpty() ? null : request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim().isEmpty() ? null : request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim().isEmpty() ? null : request.phone().trim());
        }

        user.setUpdatedAt(java.time.Instant.now());
        AppUser savedUser = appUserRepository.save(user);

        return new MeResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                buildDisplayedName(savedUser),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getPhone(),
                savedUser.getStatus(),
                savedUser.getDefaultTenant().getId(),
                savedUser.getDefaultTenant().getId(),
                savedUser.getRoles()
        );
    }
}

