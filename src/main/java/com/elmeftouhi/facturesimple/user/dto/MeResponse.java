package com.elmeftouhi.facturesimple.user.dto;

import com.elmeftouhi.facturesimple.user.Role;
import com.elmeftouhi.facturesimple.user.UserStatus;
import java.util.Set;

public record MeResponse(
        Long id,
        String email,
        String displayedName,
        String firstName,
        String lastName,
        String phone,
        UserStatus status,
        Long defaultTenantId,
        Long currentTenantId,
        Set<Role> roles
) {
}
