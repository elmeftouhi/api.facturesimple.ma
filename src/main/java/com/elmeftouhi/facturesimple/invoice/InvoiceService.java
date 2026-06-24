package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.customer.Customer;
import com.elmeftouhi.facturesimple.customer.CustomerService;
import com.elmeftouhi.facturesimple.customer.CustomerRepository;
import com.elmeftouhi.facturesimple.customer.dto.CustomerResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCreateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCustomerResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceLineItemRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceLineItemResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePageResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusChangeLogResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusUpdateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceUpdateRequest;
import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final InvoiceStatusChangeLogRepository statusChangeLogRepository;

    @Transactional
    public InvoiceResponse create(InvoiceCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();

        // Resolve customer from existing id or create inline in the same transaction.
        Customer customer = resolveCustomerForInvoice(request, tenantId);

        // Validate line items
        if (request.lineItems() == null || request.lineItems().isEmpty()) {
            throw new BadRequestException("Invoice must contain at least one line item");
        }

        Invoice invoice = new Invoice();
        // DRAFT invoices do not consume official numbering; use unique negative placeholders.
        Long draftNumber = generateNextDraftNumber(tenantId);
        String draftPlaceholder = generateDraftPlaceholder();
        invoice.setInvoiceNumber(draftNumber);
        invoice.setFormattedNumber(draftPlaceholder); // placeholder to satisfy DB NOT NULL/UNIQUE; overwritten on confirmation
        invoice.setReference(draftPlaceholder);       // placeholder to satisfy DB NOT NULL/UNIQUE; overwritten on confirmation
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate());
        invoice.setCustomer(customer);
        invoice.setDescription(normalizeNullable(request.description()));
        invoice.setVatRate(request.vatRate());
        invoice.setAmount(calculateAmount(request.lineItems()));
        invoice.setStatus(InvoiceStatus.DRAFT);

        Invoice saved = invoiceRepository.save(invoice);

        // Add line items
        for (InvoiceLineItemRequest lineReq : request.lineItems()) {
            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setInvoice(saved);
            lineItem.setItemReference(lineReq.itemReference());
            lineItem.setItemDescription(normalizeNullable(lineReq.itemDescription()));
            lineItem.setQuantity(lineReq.quantity());
            lineItem.setUnitPrice(lineReq.unitPrice());
            lineItemRepository.save(lineItem);
        }

        // Add payments if provided
        if (request.payments() != null && !request.payments().isEmpty()) {
            for (InvoicePaymentRequest paymentReq : request.payments()) {
                InvoicePayment payment = new InvoicePayment();
                payment.setInvoice(saved);
                payment.setPaymentMethod(paymentReq.paymentMethod());
                payment.setPaymentReference(normalizeNullable(paymentReq.paymentReference()));
                payment.setPaymentDate(paymentReq.paymentDate());
                payment.setPaidAmount(paymentReq.paidAmount());
                paymentRepository.save(payment);
            }
        }

        return toResponse(saved);
    }

    private Customer resolveCustomerForInvoice(InvoiceCreateRequest request, Long tenantId) {
        boolean hasCustomerId = request.customerId() != null;
        boolean hasNewCustomer = request.newCustomer() != null;

        if (hasCustomerId == hasNewCustomer) {
            throw new BadRequestException("Provide exactly one of customerId or newCustomer");
        }

        if (hasCustomerId) {
            return customerRepository.findByIdAndTenantId(request.customerId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        CustomerResponse createdCustomer = customerService.create(request.newCustomer());
        return customerRepository.findByIdAndTenantId(createdCustomer.id(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return invoiceRepository.findAllByTenantIdOrderByInvoiceDateDescIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse search(InvoiceStatus status, LocalDate fromDate, LocalDate toDate, Long customerId, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Size must be between 1 and 100");
        }

        // Apply defaults: current year range if dates not provided
        LocalDate effectiveFrom = (fromDate != null) ? fromDate : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate effectiveTo   = (toDate   != null) ? toDate   : LocalDate.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        Long tenantId = TenantContext.getRequiredTenantId();
        Specification<Invoice> baseSpec = buildSearchSpecification(tenantId, status, effectiveFrom, effectiveTo, customerId);

        // Push DRAFT-first + invoiceNumber asc sort to DB via CASE expression (correct across pages)
        Specification<Invoice> specWithOrder = (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                query.orderBy(
                    cb.asc(cb.selectCase()
                        .when(cb.equal(root.get("status"), InvoiceStatus.DRAFT), 0)
                        .otherwise(1)),
                    cb.asc(root.get("invoiceNumber"))
                );
            }
            return baseSpec.toPredicate(root, query, cb);
        };

        Pageable pageable = PageRequest.of(page, size);
        Page<Invoice> invoicePage = invoiceRepository.findAll(specWithOrder, pageable);

        return new InvoicePageResponse(
                invoicePage.getContent().stream().map(this::toResponse).toList(),
                invoicePage.getNumber(),
                invoicePage.getSize(),
                invoicePage.getTotalElements(),
                invoicePage.getTotalPages(),
                invoicePage.hasNext()
        );
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

        if (request.invoiceDate() != null) {
            invoice.setInvoiceDate(request.invoiceDate());
        }
        if (request.dueDate() != null) {
            invoice.setDueDate(request.dueDate());
        }
        if (request.description() != null) {
            invoice.setDescription(normalizeNullable(request.description()));
        }
        if (request.vatRate() != null) {
            invoice.setVatRate(request.vatRate());
        }

        return toResponse(invoice);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.DRAFT && invoice.getStatus() != InvoiceStatus.CANCELLED) {
            throw new ConflictException("Only draft or cancelled invoices can be deleted");
        }
        invoiceRepository.delete(invoice);
    }

    @Transactional
    public InvoiceResponse changeStatus(Long id, InvoiceStatusUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        InvoiceStatus oldStatus = invoice.getStatus();
        InvoiceStatus targetStatus = request.status();
        if (oldStatus == targetStatus) {
            return toResponse(invoice);
        }

        validateStatusTransition(oldStatus, targetStatus);

        // When leaving DRAFT: validate date ordering and assign the official invoice number.
        if (oldStatus == InvoiceStatus.DRAFT) {
            validateInvoiceDateOrdering(invoice.getInvoiceDate(), tenantId);
            assignOfficialNumber(invoice, tenantId);
        }

        invoice.setStatus(targetStatus);
        saveStatusChangeLog(invoice, oldStatus, targetStatus);

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceStatusChangeLogResponse> findStatusHistory(Long invoiceId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        return statusChangeLogRepository.findByInvoice_IdAndTenantIdOrderByChangedAtDesc(invoiceId, tenantId)
                .stream()
                .map(this::toStatusChangeLogResponse)
                .toList();
    }

    private void validateInvoiceDateOrdering(LocalDate invoiceDate, Long tenantId) {
        invoiceRepository.findMaxOfficialInvoiceDateByTenantId(tenantId).ifPresent(maxDate -> {
            if (invoiceDate.isBefore(maxDate)) {
                throw new ConflictException(
                    "Invoice date " + invoiceDate + " is before the most recent official invoice date " + maxDate +
                    ". You cannot backdate a confirmed invoice."
                );
            }
        });
    }

    private void assignOfficialNumber(Invoice invoice, Long tenantId) {
        Long nextNumber = invoiceRepository.getMaxInvoiceNumberForTenant(tenantId) + 1;
        String formatted = generateFormattedNumber(nextNumber, invoice.getInvoiceDate().getYear());
        invoice.setInvoiceNumber(nextNumber);
        invoice.setFormattedNumber(formatted);
        invoice.setReference(formatted);
    }

    private Long generateNextDraftNumber(Long tenantId) {
        Long minNumber = invoiceRepository.getMinInvoiceNumberForTenant(tenantId);
        return minNumber > 0 ? -1L : minNumber - 1L;
    }

    private String generateDraftPlaceholder() {
        return "DRAFT-" + UUID.randomUUID();
    }

    private void saveStatusChangeLog(Invoice invoice, InvoiceStatus oldStatus, InvoiceStatus newStatus) {
        InvoiceStatusChangeLog log = new InvoiceStatusChangeLog();
        log.setInvoice(invoice);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setCreatedBy(resolveCurrentUsername());
        statusChangeLogRepository.save(log);
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal && jwtPrincipal.username() != null && !jwtPrincipal.username().isBlank()) {
            return jwtPrincipal.username();
        }
        if (principal instanceof String principalName && !principalName.isBlank() && !"anonymousUser".equalsIgnoreCase(principalName)) {
            return principalName;
        }

        String authName = authentication.getName();
        if (authName != null && !authName.isBlank() && !"anonymousUser".equalsIgnoreCase(authName)) {
            return authName;
        }

        return "system";
    }

    private InvoiceStatusChangeLogResponse toStatusChangeLogResponse(InvoiceStatusChangeLog log) {
        return new InvoiceStatusChangeLogResponse(
                log.getChangedAt(),
                log.getCreatedBy(),
                log.getOldStatus(),
                log.getNewStatus()
        );
    }

    @Transactional
    public InvoicePaymentResponse addPayment(Long invoiceId, InvoicePaymentRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        InvoicePayment payment = new InvoicePayment();
        payment.setInvoice(invoice);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentReference(normalizeNullable(request.paymentReference()));
        payment.setPaymentDate(request.paymentDate());
        payment.setPaidAmount(request.paidAmount());

        InvoicePayment saved = paymentRepository.save(payment);
        return toPaymentResponse(saved);
    }

    @Transactional
    public void removePayment(Long invoiceId, Long paymentId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        InvoicePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getInvoice().getId().equals(invoice.getId())) {
            throw new BadRequestException("Payment does not belong to this invoice");
        }

        paymentRepository.delete(payment);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineItemResponse> lineItems = lineItemRepository.findByInvoiceIdOrderByIdAsc(invoice.getId())
                .stream()
                .map(this::toLineItemResponse)
                .toList();

        List<InvoicePaymentResponse> payments = paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoice.getId())
                .stream()
                .map(this::toPaymentResponse)
                .toList();

        BigDecimal paidAmount = calculatePaidAmount(payments);
        BigDecimal remainingAmount = calculateRemainingAmount(invoice.getAmount(), paidAmount);

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getFormattedNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                toCustomerResponse(invoice.getCustomer()),
                invoice.getDescription(),
                invoice.getVatRate(),
                paidAmount,
                remainingAmount,
                resolvePaymentStatus(invoice.getAmount(), remainingAmount),
                invoice.getStatus(),
                lineItems,
                payments,
                invoice.getTenantId(),
                invoice.getCreatedAt()
        );
    }

    private Specification<Invoice> buildSearchSpecification(
            Long tenantId,
            InvoiceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Long customerId
    ) {
        Specification<Invoice> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("invoiceDate"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("invoiceDate"), toDate));
        }
        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }

        return spec;
    }

    private BigDecimal calculatePaidAmount(List<InvoicePaymentResponse> payments) {
        return payments.stream()
                .map(InvoicePaymentResponse::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRemainingAmount(BigDecimal invoiceAmount, BigDecimal paidAmount) {
        return invoiceAmount.subtract(paidAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private InvoicePaymentStatus resolvePaymentStatus(BigDecimal invoiceAmount, BigDecimal remainingAmount) {
        if (remainingAmount.compareTo(invoiceAmount) == 0) {
            return InvoicePaymentStatus.UNPAID;
        }
        if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            return InvoicePaymentStatus.PAID;
        }
        return InvoicePaymentStatus.PARTIAL;
    }

    private InvoiceCustomerResponse toCustomerResponse(Customer customer) {
        return new InvoiceCustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getTaxId(),
                customer.getCategory() != null ? customer.getCategory().getId() : null,
                customer.getCategory() != null ? customer.getCategory().getName() : null
        );
    }

    private InvoiceLineItemResponse toLineItemResponse(InvoiceLineItem lineItem) {
        BigDecimal lineTotal = lineItem.getQuantity().multiply(lineItem.getUnitPrice());
        return new InvoiceLineItemResponse(
                lineItem.getId(),
                lineItem.getItemReference(),
                lineItem.getItemDescription(),
                lineItem.getQuantity(),
                lineItem.getUnitPrice(),
                lineTotal
        );
    }

    private InvoicePaymentResponse toPaymentResponse(InvoicePayment payment) {
        return new InvoicePaymentResponse(
                payment.getId(),
                payment.getPaymentMethod(),
                payment.getPaymentReference(),
                payment.getPaymentDate(),
                payment.getPaidAmount()
        );
    }

    private String generateFormattedNumber(Long invoiceNumber, int year) {
        return invoiceNumber + "-" + year;
    }

    private BigDecimal calculateAmount(List<InvoiceLineItemRequest> lineItems) {
        return lineItems.stream()
                .map(line -> line.quantity().multiply(line.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateStatusTransition(InvoiceStatus current, InvoiceStatus target) {
        boolean valid = switch (current) {
            case DRAFT -> target == InvoiceStatus.PRINTED || target == InvoiceStatus.CANCELLED;
            case PRINTED -> target == InvoiceStatus.DRAFT || target == InvoiceStatus.SOLD || target == InvoiceStatus.CANCELLED;
            case SOLD -> target == InvoiceStatus.ARCHIVED;
            case CANCELLED -> target == InvoiceStatus.ARCHIVED;
            case ARCHIVED -> target == InvoiceStatus.FINAL;
            case FINAL -> false;
        };

        if (!valid) {
            throw new ConflictException("Invalid invoice status transition from " + current + " to " + target);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

