package com.elmeftouhi.facturesimple.invoice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceStatusRepository extends JpaRepository<InvoiceStatus, Long> {

    List<InvoiceStatus> findAllByTenantIdOrderByDisplayOrderAsc(Long tenantId);

    List<InvoiceStatus> findAllByTenantIdAndActiveTrueOrderByDisplayOrderAsc(Long tenantId);

    Optional<InvoiceStatus> findByIdAndTenantId(Long id, Long tenantId);

    Optional<InvoiceStatus> findByNameAndTenantId(String name, Long tenantId);

    boolean existsByNameAndTenantId(String name, Long tenantId);
}
