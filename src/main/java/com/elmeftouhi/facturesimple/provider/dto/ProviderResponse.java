package com.elmeftouhi.facturesimple.provider.dto;

import java.time.Instant;

public record ProviderResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String taxId,
        Long categoryId,
        String categoryName,
        Long tenantId,
        Instant createdAt
) {
}

