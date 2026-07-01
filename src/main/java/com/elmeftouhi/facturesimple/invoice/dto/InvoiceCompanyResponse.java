package com.elmeftouhi.facturesimple.invoice.dto;

import java.math.BigDecimal;

public record InvoiceCompanyResponse(
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
        Integer paymentTermsInDays
) {
}

