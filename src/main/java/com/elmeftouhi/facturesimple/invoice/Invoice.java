package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.customer.Customer;
import com.elmeftouhi.facturesimple.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = @UniqueConstraint(name = "uk_invoice_number_tenant", columnNames = {"invoice_number", "tenant_id"}),
        indexes = {
                @Index(name = "idx_invoices_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_invoices_customer_id", columnList = "customer_id"),
                @Index(name = "idx_invoices_date", columnList = "invoice_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null for DRAFT invoices; assigned when transitioning out of DRAFT.
    @Column(name = "invoice_number")
    private Long invoiceNumber;

    // Null for DRAFT invoices; assigned when transitioning out of DRAFT.
    @Column(name = "formatted_number", length = 80)
    private String formattedNumber;

    // Null for DRAFT invoices; assigned when transitioning out of DRAFT.
    @Column(length = 80)
    private String reference;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(length = 500)
    private String description;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    // Kept for compatibility with existing schema; calculated from line items.
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_used", length = 30)
    private InvoiceTemplate templateUsed = InvoiceTemplate.CLASSIC;

    @Column(nullable = false, length = 80)
    private String status = "DRAFT";

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoicePayment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceDiscount> discounts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercice_id")
    private com.elmeftouhi.facturesimple.exercice.Exercice exercice;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_invoice_number")
    private Long deletedInvoiceNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

