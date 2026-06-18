package com.elmeftouhi.facturesimple.tenant.dto;

public record TenantMembershipResponse(
        Long id,
        String name,
        boolean isDefault
) {
}

