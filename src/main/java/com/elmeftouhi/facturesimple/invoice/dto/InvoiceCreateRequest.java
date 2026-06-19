package com.elmeftouhi.facturesimple.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceCreateRequest(
        @NotNull Long customerId,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal vatRate,
        List<InvoiceLineItemRequest> lineItems,
        List<InvoicePaymentRequest> payments
) {
}

