package com.elmeftouhi.facturesimple.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InvoiceUpdateRequest(
        @NotBlank @Size(max = 80) String reference,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
) {
}

