package com.elmeftouhi.facturesimple.provider.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @Email @Size(max = 190) String email,
        @Size(max = 40) String phone,
        @Size(max = 500) String address,
        @Size(max = 80) String taxId,
        Long categoryId
) {
}

