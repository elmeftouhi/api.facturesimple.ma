package com.elmeftouhi.facturesimple.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
        Long id,
        String reference,
        String description,
        BigDecimal amount,
        Long tenantId,
        Instant createdAt
) {
}

