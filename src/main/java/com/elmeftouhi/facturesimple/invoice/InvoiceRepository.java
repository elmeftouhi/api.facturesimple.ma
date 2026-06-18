package com.elmeftouhi.facturesimple.invoice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByTenantIdOrderByIdDesc(Long tenantId);

    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByReferenceAndTenantId(String reference, Long tenantId);
}

