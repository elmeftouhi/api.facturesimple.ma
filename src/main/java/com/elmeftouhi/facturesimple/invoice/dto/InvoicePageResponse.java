package com.elmeftouhi.facturesimple.invoice.dto;

import java.util.List;

public record InvoicePageResponse(
        List<InvoiceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}

