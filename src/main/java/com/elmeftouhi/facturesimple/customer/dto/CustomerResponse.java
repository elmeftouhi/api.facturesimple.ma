package com.elmeftouhi.facturesimple.customer.dto;

import java.time.Instant;

public record CustomerResponse(
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

