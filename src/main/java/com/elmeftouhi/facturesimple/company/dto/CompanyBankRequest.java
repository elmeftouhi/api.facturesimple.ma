package com.elmeftouhi.facturesimple.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyBankRequest(
        @NotBlank @Size(max = 120) String bankName,
        @Size(max = 80) String accountNumber,
        @Size(max = 20) String swiftCode,
        @Size(max = 80) String iban,
        Boolean isDefault
) {
}

