package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.InvoiceStatusCategory;
import java.util.Map;

public record InvoiceStatusResponse(
        Long id,
        String name,
        String label,
        String color,
        InvoiceStatusCategory category,
        int displayOrder,
        boolean active,
        Map<String, String> labels
) {
}
