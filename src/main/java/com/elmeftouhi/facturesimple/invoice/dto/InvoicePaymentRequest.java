package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoicePaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 120) String paymentReference,
        @NotNull LocalDate paymentDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal paidAmount,
        @Size(max = 120) String bankName
) {
}

