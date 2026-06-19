package com.elmeftouhi.facturesimple.customer.category.dto;

import java.time.Instant;

public record CustomerCategoryResponse(
        Long id,
        String name,
        String description,
        boolean isDefault,
        Long tenantId,
        Instant createdAt
) {
}

