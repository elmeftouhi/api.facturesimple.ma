package com.elmeftouhi.facturesimple.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceUpdateRequest(
        LocalDate invoiceDate,
        LocalDate dueDate,
        @Size(max = 500) String description,
        @DecimalMin(value = "0.01") BigDecimal vatRate
) {
}

