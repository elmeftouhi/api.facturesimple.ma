package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.DiscountType;
import java.math.BigDecimal;

public record InvoiceDiscountResponse(
        Long id,
        String name,
        DiscountType discountType,
        BigDecimal discountValue
) {}
