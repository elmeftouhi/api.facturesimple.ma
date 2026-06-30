package com.elmeftouhi.facturesimple.company;

import com.elmeftouhi.facturesimple.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "companies",
        uniqueConstraints = @UniqueConstraint(name = "uk_company_tenant", columnNames = {"tenant_id"}),
        indexes = {
                @Index(name = "idx_companies_tenant_id", columnList = "tenant_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Company extends BaseTenantAwareEntity {

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

    @Column(name = "registre_commerce", length = 80)
    private String registreCommerce;

    @Column(length = 500)
    private String logo;

    @Column(length = 255)
    private String website;

    @Column(length = 3)
    private String currency = "MAD";

    @Column(name = "default_vat_rate", precision = 5, scale = 2)
    private BigDecimal defaultVatRate;

    @Column(name = "payment_terms_in_days")
    private Integer paymentTermsInDays;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompanyBank> banks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

