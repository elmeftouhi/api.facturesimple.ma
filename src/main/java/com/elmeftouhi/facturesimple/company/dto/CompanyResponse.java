package com.elmeftouhi.facturesimple.company.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CompanyResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String taxId,
        String registreCommerce,
        String logo,
        String website,
        String currency,
        String language,
        BigDecimal defaultVatRate,
        Integer paymentTermsInDays,
        String description,
        List<CompanyBankResponse> banks,
        Long tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}

