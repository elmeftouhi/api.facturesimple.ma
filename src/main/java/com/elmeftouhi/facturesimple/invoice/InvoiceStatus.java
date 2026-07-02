package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "invoice_statuses",
        uniqueConstraints = @UniqueConstraint(name = "uk_status_name_tenant", columnNames = {"name", "tenant_id"}),
        indexes = {
                @Index(name = "idx_invoice_statuses_tenant_id", columnList = "tenant_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InvoiceStatus extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(length = 20)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatusCategory category;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_status_labels", joinColumns = @JoinColumn(name = "status_id"))
    @MapKeyColumn(name = "locale")
    @Column(name = "label", nullable = false)
    private Map<String, String> labels = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
