package com.elmeftouhi.facturesimple.company;

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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "company_banks",
        indexes = {
                @Index(name = "idx_company_banks_company_id", columnList = "company_id"),
                @Index(name = "idx_company_banks_is_default", columnList = "company_id, is_default")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CompanyBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 120)
    private String bankName;

    @Column(length = 80)
    private String accountNumber;

    @Column(length = 20)
    private String swiftCode;

    @Column(length = 80)
    private String iban;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

