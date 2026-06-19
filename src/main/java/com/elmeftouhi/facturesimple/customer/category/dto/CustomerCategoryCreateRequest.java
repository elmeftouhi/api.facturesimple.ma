package com.elmeftouhi.facturesimple.customer.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerCategoryCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        Boolean isDefault
) {
}

