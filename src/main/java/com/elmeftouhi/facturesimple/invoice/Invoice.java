package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = @UniqueConstraint(name = "uk_invoice_reference_tenant", columnNames = {"reference", "tenant_id"}),
        indexes = @Index(name = "idx_invoices_tenant_id", columnList = "tenant_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String reference;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

