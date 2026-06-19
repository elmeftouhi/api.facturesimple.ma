package com.elmeftouhi.facturesimple.provider.category.dto;

import java.time.Instant;

public record ProviderCategoryResponse(
        Long id,
        String name,
        String description,
        boolean isDefault,
        Long tenantId,
        Instant createdAt
) {
}

