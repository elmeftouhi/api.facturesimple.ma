package com.elmeftouhi.facturesimple.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CompanyCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @Email @Size(max = 190) String email,
        @Size(max = 40) String phone,
        @Size(max = 500) String address,
        @Size(max = 80) String taxId,
        @Size(max = 80) String registreCommerce,
        @Size(max = 500) String logo,
        @Size(max = 255) String website,
        @Size(max = 3) String currency,
        @Size(max = 10) String language,
        BigDecimal defaultVatRate,
        Integer paymentTermsInDays,
        @Size(max = 500) String description
) {
}

