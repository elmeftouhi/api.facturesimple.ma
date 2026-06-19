package com.elmeftouhi.facturesimple.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
        @Size(max = 120) String name,
        @Email @Size(max = 190) String email,
        @Size(max = 40) String phone,
        @Size(max = 500) String address,
        @Size(max = 80) String taxId,
        Long categoryId
) {
}

