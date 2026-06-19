package com.elmeftouhi.facturesimple.provider.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderCategoryCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        Boolean isDefault
) {
}

