package com.elmeftouhi.facturesimple.invoice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    List<Invoice> findAllByTenantIdAndDeletedAtIsNullOrderByInvoiceDateDescIdDesc(Long tenantId);

    Optional<Invoice> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    Optional<Invoice> findByInvoiceNumberAndTenantIdAndDeletedAtIsNull(Long invoiceNumber, Long tenantId);

    Optional<Invoice> findByFormattedNumberAndTenantIdAndDeletedAtIsNull(String formattedNumber, Long tenantId);

    boolean existsByInvoiceNumberAndTenantIdAndDeletedAtIsNull(Long invoiceNumber, Long tenantId);

    boolean existsByStatusAndTenantIdAndDeletedAtIsNull(String status, Long tenantId);

    // Official numbers are scoped per Exercice and ignore soft-deleted rows.
    @Query("SELECT COALESCE(MAX(i.invoiceNumber), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.exercice.id = :exerciceId AND i.deletedAt IS NULL AND i.status != 'DRAFT'")
    Long getMaxOfficialInvoiceNumberForExercice(@Param("tenantId") Long tenantId, @Param("exerciceId") Long exerciceId);

    // Latest invoice date among official (non-DRAFT) invoices in the same Exercice.
    @Query("SELECT MAX(i.invoiceDate) FROM Invoice i WHERE i.tenantId = :tenantId AND i.exercice.id = :exerciceId AND i.deletedAt IS NULL AND i.status != 'DRAFT'")
    Optional<LocalDate> findMaxOfficialInvoiceDateByTenantIdAndExerciceId(@Param("tenantId") Long tenantId, @Param("exerciceId") Long exerciceId);

    @Query("SELECT COALESCE(MIN(i.invoiceNumber), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL")
    Long getMinInvoiceNumberForTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(MIN(i.invoiceNumber), 0) FROM Invoice i WHERE i.tenantId = :tenantId")
    Long getMinInvoiceNumberForTenantIncludingDeleted(@Param("tenantId") Long tenantId);

    Optional<Invoice> findTopByTenantIdAndExercice_IdAndDeletedAtIsNotNullAndDeletedInvoiceNumberIsNotNullOrderByDeletedAtDesc(
            Long tenantId,
            Long exerciceId
    );
}

