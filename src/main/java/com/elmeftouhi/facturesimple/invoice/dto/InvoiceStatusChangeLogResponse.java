package com.elmeftouhi.facturesimple.invoice.dto;

import java.time.Instant;

public record InvoiceStatusChangeLogResponse(
        Instant changedAt,
        String createdBy,
        String oldStatus,
        String newStatus
) {
}

