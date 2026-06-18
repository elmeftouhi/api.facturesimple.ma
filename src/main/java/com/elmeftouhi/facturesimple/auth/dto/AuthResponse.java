package com.elmeftouhi.facturesimple.auth.dto;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMinutes,
        Long selectedTenantId,
        Set<Long> allowedTenantIds
) {
}

