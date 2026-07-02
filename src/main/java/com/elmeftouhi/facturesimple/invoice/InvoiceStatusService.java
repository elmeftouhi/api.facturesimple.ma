package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusSettingsCreateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusSettingsUpdateRequest;
import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceStatusService {

    private final InvoiceStatusRepository statusRepository;
    private final InvoiceRepository invoiceRepository;

    private static final Set<String> SYSTEM_STATUSES = Set.of("DRAFT", "PRINTED", "SOLD", "CANCELLED", "ARCHIVED", "FINAL");

    @Transactional(readOnly = true)
    public List<InvoiceStatusResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return statusRepository.findAllByTenantIdOrderByDisplayOrderAsc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceStatusResponse> findAllActive() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return statusRepository.findAllByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvoiceStatusResponse create(InvoiceStatusSettingsCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        String nameUpper = request.name().trim().toUpperCase();

        if (statusRepository.existsByNameAndTenantId(nameUpper, tenantId)) {
            throw new ConflictException("Invoice status with name '" + nameUpper + "' already exists");
        }

        InvoiceStatus status = new InvoiceStatus();
        status.setName(nameUpper);
        status.setLabel(request.label().trim());
        status.setColor(request.color() != null ? request.color().trim() : null);
        status.setCategory(request.category());
        status.setDisplayOrder(request.displayOrder());
        status.setActive(true);

        if (request.labels() != null) {
            status.getLabels().putAll(request.labels());
        }

        InvoiceStatus saved = statusRepository.save(status);
        return toResponse(saved);
    }

    @Transactional
    public InvoiceStatusResponse update(Long id, InvoiceStatusSettingsUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        InvoiceStatus status = statusRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice status not found"));

        boolean isSystem = SYSTEM_STATUSES.contains(status.getName());

        if (isSystem) {
            if (status.getCategory() != request.category()) {
                throw new BadRequestException("Cannot change category of system status " + status.getName());
            }
            if (!request.active()) {
                throw new BadRequestException("Cannot deactivate system status " + status.getName());
            }
        }

        status.setLabel(request.label().trim());
        status.setColor(request.color() != null ? request.color().trim() : null);
        status.setCategory(request.category());
        status.setDisplayOrder(request.displayOrder());
        status.setActive(request.active());

        status.getLabels().clear();
        if (request.labels() != null) {
            status.getLabels().putAll(request.labels());
        }

        InvoiceStatus saved = statusRepository.save(status);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        InvoiceStatus status = statusRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice status not found"));

        if (SYSTEM_STATUSES.contains(status.getName())) {
            throw new BadRequestException("Cannot delete system status " + status.getName());
        }

        boolean inUse = invoiceRepository.existsByStatusAndTenantId(status.getName(), tenantId);
        if (inUse) {
            throw new ConflictException("Cannot delete status because it is currently in use by one or more invoices. You can deactivate it instead.");
        }

        statusRepository.delete(status);
    }

    @Transactional
    public void initializeDefaultStatuses(Long tenantId) {
        if (!statusRepository.findAllByTenantIdOrderByDisplayOrderAsc(tenantId).isEmpty()) {
            return;
        }

        createDefaultStatus(tenantId, "DRAFT", "Draft", "#94a3b8", InvoiceStatusCategory.DRAFT, 1,
                Map.of("en", "Draft", "fr", "Brouillon", "es", "Borrador", "ar", "مسودة"));

        createDefaultStatus(tenantId, "PRINTED", "Printed", "#38bdf8", InvoiceStatusCategory.CONFIRMED, 2,
                Map.of("en", "Printed", "fr", "Imprimée", "es", "Impresa", "ar", "مطبوعة"));

        createDefaultStatus(tenantId, "SOLD", "Sold", "#10b981", InvoiceStatusCategory.CONFIRMED, 3,
                Map.of("en", "Sold", "fr", "Vendue", "es", "Vendida", "ar", "مبيعة"));

        createDefaultStatus(tenantId, "CANCELLED", "Cancelled", "#ef4444", InvoiceStatusCategory.CANCELLED, 4,
                Map.of("en", "Cancelled", "fr", "Annulée", "es", "Cancelada", "ar", "ملغاة"));

        createDefaultStatus(tenantId, "ARCHIVED", "Archived", "#8b5cf6", InvoiceStatusCategory.CLOSED, 5,
                Map.of("en", "Archived", "fr", "Archivée", "es", "Archivada", "ar", "مؤرشفة"));

        createDefaultStatus(tenantId, "FINAL", "Final", "#059669", InvoiceStatusCategory.CLOSED, 6,
                Map.of("en", "Final", "fr", "Finalisée", "es", "Finalizada", "ar", "نهائية"));
    }

    private void createDefaultStatus(Long tenantId, String name, String label, String color, InvoiceStatusCategory category, int order, Map<String, String> labels) {
        InvoiceStatus status = new InvoiceStatus();
        status.assignTenantId(tenantId);
        status.setName(name);
        status.setLabel(label);
        status.setColor(color);
        status.setCategory(category);
        status.setDisplayOrder(order);
        status.setActive(true);
        status.getLabels().putAll(labels);
        statusRepository.save(status);
    }

    public InvoiceStatusResponse toResponse(InvoiceStatus status) {
        return new InvoiceStatusResponse(
                status.getId(),
                status.getName(),
                status.getLabel(),
                status.getColor(),
                status.getCategory(),
                status.getDisplayOrder(),
                status.isActive(),
                new HashMap<>(status.getLabels())
        );
    }
}
