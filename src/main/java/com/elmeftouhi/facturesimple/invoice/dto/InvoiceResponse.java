package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.InvoicePaymentStatus;
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
        InvoiceCompanyResponse company,
        com.elmeftouhi.facturesimple.invoice.dto.InvoiceCustomerResponse customer,
        String description,
        BigDecimal vatRate,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        InvoicePaymentStatus paymentStatus,
        InvoiceStatus status,
        List<InvoiceLineItemResponse> lineItems,
        List<InvoicePaymentResponse> payments,
        Long tenantId,
        Instant createdAt,
        Long exerciceId,
        String exerciceName
) {
}
