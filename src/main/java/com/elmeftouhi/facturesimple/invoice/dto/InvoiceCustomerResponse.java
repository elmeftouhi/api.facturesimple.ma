package com.elmeftouhi.facturesimple.invoice.dto;

public record InvoiceCustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String taxId,
        Long categoryId,
        String categoryName
) {
}

