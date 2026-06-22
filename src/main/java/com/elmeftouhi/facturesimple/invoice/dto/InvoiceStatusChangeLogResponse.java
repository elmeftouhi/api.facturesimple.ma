package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.InvoiceStatus;
import java.time.Instant;

public record InvoiceStatusChangeLogResponse(
        Instant changedAt,
        String createdBy,
        InvoiceStatus oldStatus,
        InvoiceStatus newStatus
) {
}

