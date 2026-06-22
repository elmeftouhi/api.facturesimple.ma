package com.elmeftouhi.facturesimple.invoice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceStatusChangeLogRepository extends JpaRepository<InvoiceStatusChangeLog, Long> {

    List<InvoiceStatusChangeLog> findByInvoice_IdAndTenantIdOrderByChangedAtDesc(Long invoiceId, Long tenantId);
}

