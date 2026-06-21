package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record InvoiceStatusUpdateRequest(
        @NotNull InvoiceStatus status
) {
}

