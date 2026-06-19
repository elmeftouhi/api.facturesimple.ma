package com.elmeftouhi.facturesimple.user;

import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.facturesimple.tenant.dto.TenantMembershipResponse;
import com.elmeftouhi.facturesimple.user.dto.MeResponse;
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

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDefaultTenant().getId(),
                principal.selectedTenantId(),
                user.getRoles()
        );
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
}

