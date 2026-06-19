package com.elmeftouhi.facturesimple.invoice.dto;

import java.math.BigDecimal;

public record InvoiceLineItemResponse(
        Long id,
        String itemReference,
        String itemDescription,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}

