package com.elmeftouhi.facturesimple.invoice.dto;

import jakarta.validation.constraints.NotBlank;

public record InvoiceStatusUpdateRequest(
        @NotBlank String status
) {
}
