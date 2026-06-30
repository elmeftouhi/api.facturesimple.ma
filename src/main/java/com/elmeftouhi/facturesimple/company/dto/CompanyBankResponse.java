package com.elmeftouhi.facturesimple.company.dto;

import java.time.Instant;

public record CompanyBankResponse(
        Long id,
        String bankName,
        String accountNumber,
        String swiftCode,
        String iban,
        Boolean isDefault,
        Instant createdAt
) {
}

