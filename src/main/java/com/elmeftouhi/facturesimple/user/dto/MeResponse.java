package com.elmeftouhi.facturesimple.user.dto;

import com.elmeftouhi.facturesimple.user.Role;
import java.util.Set;

public record MeResponse(
        Long id,
        String email,
        Long defaultTenantId,
        Long currentTenantId,
        Set<Role> roles
) {
}

