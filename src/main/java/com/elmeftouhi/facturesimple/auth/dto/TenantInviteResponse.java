package com.elmeftouhi.facturesimple.auth.dto;

import java.time.Instant;

public record TenantInviteResponse(
        String inviteCode,
        Long tenantId,
        Instant expiresAt
) {
}

