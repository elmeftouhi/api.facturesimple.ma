package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        Long invoiceNumber,
        String formattedNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        Long customerId,
        String customerName,
        String description,
        BigDecimal vatRate,
        InvoiceStatus status,
        List<InvoiceLineItemResponse> lineItems,
        List<InvoicePaymentResponse> payments,
        Long tenantId,
        Instant createdAt
) {
}

