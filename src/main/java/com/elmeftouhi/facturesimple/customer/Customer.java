package com.elmeftouhi.facturesimple.customer;

import com.elmeftouhi.facturesimple.customer.category.CustomerCategory;
import com.elmeftouhi.facturesimple.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_name_tenant", columnNames = {"name", "tenant_id"}),
        indexes = {
                @Index(name = "idx_customers_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_customers_category_id", columnList = "category_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Customer extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 190)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(name = "tax_id", length = 80)
    private String taxId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CustomerCategory category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

