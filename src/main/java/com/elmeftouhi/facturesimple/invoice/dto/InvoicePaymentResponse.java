package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoicePaymentResponse(
        Long id,
        PaymentMethod paymentMethod,
        String paymentReference,
        LocalDate paymentDate,
        BigDecimal paidAmount,
        String bankName
) {
}

