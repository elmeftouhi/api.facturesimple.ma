package com.elmeftouhi.facturesimple.customer.category.dto;

import jakarta.validation.constraints.Size;

public record CustomerCategoryUpdateRequest(
        @Size(max = 120) String name,
        @Size(max = 500) String description
) {
}

