package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "invoice_status_change_logs",
        indexes = {
                @Index(name = "idx_invoice_status_logs_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_invoice_status_logs_invoice_id", columnList = "invoice_id"),
                @Index(name = "idx_invoice_status_logs_changed_at", columnList = "changed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InvoiceStatusChangeLog extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false, length = 20)
    private InvoiceStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private InvoiceStatus newStatus;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();
}

