package com.elmeftouhi.facturesimple.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InvoiceLineItemRequest(
        @NotBlank @Size(max = 120) String itemReference,
        @Size(max = 500) String itemDescription,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice
) {
}

