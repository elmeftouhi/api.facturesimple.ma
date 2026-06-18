package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCreateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceUpdateRequest;
import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public InvoiceResponse create(InvoiceCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        if (invoiceRepository.existsByReferenceAndTenantId(request.reference(), tenantId)) {
            throw new ConflictException("Invoice reference already exists in this tenant");
        }

        Invoice invoice = new Invoice();
        invoice.setReference(request.reference());
        invoice.setDescription(request.description());
        invoice.setAmount(request.amount());

        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return invoiceRepository.findAllByTenantIdOrderByIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse update(Long id, InvoiceUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getReference().equals(request.reference())
                && invoiceRepository.existsByReferenceAndTenantId(request.reference(), tenantId)) {
            throw new ConflictException("Invoice reference already exists in this tenant");
        }

        invoice.setReference(request.reference());
        invoice.setDescription(request.description());
        invoice.setAmount(request.amount());

        return toResponse(invoice);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        invoiceRepository.delete(invoice);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getReference(),
                invoice.getDescription(),
                invoice.getAmount(),
                invoice.getTenantId(),
                invoice.getCreatedAt()
        );
    }
}

