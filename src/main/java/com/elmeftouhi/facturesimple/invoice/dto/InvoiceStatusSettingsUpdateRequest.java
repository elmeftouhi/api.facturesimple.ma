package com.elmeftouhi.facturesimple.invoice.dto;

import com.elmeftouhi.facturesimple.invoice.InvoiceStatusCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record InvoiceStatusSettingsUpdateRequest(
        @NotBlank @Size(max = 80) String label,
        @Size(max = 20) String color,
        @NotNull InvoiceStatusCategory category,
        int displayOrder,
        boolean active,
        Map<String, String> labels
) {
}
