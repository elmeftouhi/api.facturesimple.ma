package com.elmeftouhi.facturesimple.invoice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByTenantIdOrderByInvoiceDateDescIdDesc(Long tenantId);

    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByInvoiceNumberAndTenantId(Long invoiceNumber, Long tenantId);

    @Query("SELECT COALESCE(MAX(i.invoiceNumber), 0) FROM Invoice i WHERE i.tenantId = :tenantId")
    Long getMaxInvoiceNumberForTenant(@Param("tenantId") Long tenantId);
}

