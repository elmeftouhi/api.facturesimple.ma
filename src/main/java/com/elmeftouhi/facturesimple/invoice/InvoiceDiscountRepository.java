package com.elmeftouhi.facturesimple.invoice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDiscountRepository extends JpaRepository<InvoiceDiscount, Long> {

    List<InvoiceDiscount> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
