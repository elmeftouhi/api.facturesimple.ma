package com.elmeftouhi.facturesimple.auth.dto;

import jakarta.validation.constraints.NotNull;

public record JoinTenantRequest(@NotNull Long tenantId) {
}

