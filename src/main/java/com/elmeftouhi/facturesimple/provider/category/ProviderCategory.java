package com.elmeftouhi.facturesimple.provider.category;

import com.elmeftouhi.facturesimple.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "provider_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_provider_category_name_tenant", columnNames = {"name", "tenant_id"}),
        indexes = @Index(name = "idx_provider_categories_tenant_id", columnList = "tenant_id")
)
@Getter
@Setter
@NoArgsConstructor
public class ProviderCategory extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_default", nullable = false)
    private boolean defaultCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

