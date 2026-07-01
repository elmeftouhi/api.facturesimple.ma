package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.customer.dto.CustomerCreateRequest;
import com.elmeftouhi.facturesimple.invoice.InvoiceTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceCreateRequest(
        Long customerId,
        @Valid CustomerCreateRequest newCustomer,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal vatRate,
        InvoiceTemplate templateChoice,
        List<InvoiceLineItemRequest> lineItems,
        List<InvoicePaymentRequest> payments
) {
}

