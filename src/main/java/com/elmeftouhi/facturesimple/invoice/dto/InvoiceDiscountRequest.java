package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InvoiceDiscountRequest(
        @NotNull @Size(max = 120) String name,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin(value = "0.00") BigDecimal discountValue
) {}
