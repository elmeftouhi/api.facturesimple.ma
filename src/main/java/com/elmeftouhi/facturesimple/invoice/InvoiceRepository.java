package com.elmeftouhi.facturesimple.invoice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    List<Invoice> findAllByTenantIdOrderByInvoiceDateDescIdDesc(Long tenantId);

    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Invoice> findByInvoiceNumberAndTenantId(Long invoiceNumber, Long tenantId);

    Optional<Invoice> findByFormattedNumberAndTenantId(String formattedNumber, Long tenantId);

    boolean existsByInvoiceNumberAndTenantId(Long invoiceNumber, Long tenantId);

    // Only counts official (non-DRAFT) invoices to avoid gaps when drafts are deleted.
    @Query("SELECT COALESCE(MAX(i.invoiceNumber), 0) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status != com.elmeftouhi.facturesimple.invoice.InvoiceStatus.DRAFT")
    Long getMaxInvoiceNumberForTenant(@Param("tenantId") Long tenantId);

    // Latest invoice date among official (non-DRAFT) invoices for date-ordering validation.
    @Query("SELECT MAX(i.invoiceDate) FROM Invoice i WHERE i.tenantId = :tenantId AND i.status != com.elmeftouhi.facturesimple.invoice.InvoiceStatus.DRAFT")
    Optional<LocalDate> findMaxOfficialInvoiceDateByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(MIN(i.invoiceNumber), 0) FROM Invoice i WHERE i.tenantId = :tenantId")
    Long getMinInvoiceNumberForTenant(@Param("tenantId") Long tenantId);
}

