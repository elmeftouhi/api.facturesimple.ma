package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.company.Company;
import com.elmeftouhi.facturesimple.company.CompanyRepository;
import com.elmeftouhi.facturesimple.user.AppUserRepository;
import com.elmeftouhi.facturesimple.customer.Customer;
import com.elmeftouhi.facturesimple.customer.CustomerService;
import com.elmeftouhi.facturesimple.customer.CustomerRepository;
import com.elmeftouhi.facturesimple.customer.dto.CustomerResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCompanyResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCreateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCustomerResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceDiscountRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceDiscountResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceLineItemRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceLineItemResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePageResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusChangeLogResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusUpdateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceUpdateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusResponse;
import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elmeftouhi.facturesimple.exercice.Exercice;
import com.elmeftouhi.facturesimple.exercice.ExerciceStatus;
import com.elmeftouhi.facturesimple.exercice.ExerciceRepository;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final InvoiceStatusChangeLogRepository statusChangeLogRepository;
    private final CompanyRepository companyRepository;
    private final ExerciceRepository exerciceRepository;
    private final AppUserRepository appUserRepository;
    private final InvoiceDiscountRepository invoiceDiscountRepository;
    private final InvoiceStatusRepository statusRepository;
    private final InvoiceStatusService statusService;

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
        // Resolve active Exercice for the invoice date
        LocalDate invoiceDate = request.invoiceDate();
        Exercice exercice = exerciceRepository.findExerciceForDate(tenantId, invoiceDate)
                .orElseThrow(() -> new BadRequestException("No fiscal year (exercice) defined for the invoice date " + invoiceDate + ". Please open one first."));
        if (exercice.getStatus() == ExerciceStatus.CLOSED) {
            throw new BadRequestException("The fiscal year (exercice) " + exercice.getName() + " is closed. Cannot create invoices in it.");
        }
        invoice.setExercice(exercice);

        // DRAFT invoices do not consume official numbering; use unique negative placeholders.
        Long draftNumber = generateNextDraftNumber(tenantId);
        String draftPlaceholder = generateDraftPlaceholder();
        invoice.setInvoiceNumber(draftNumber);
        invoice.setFormattedNumber(draftPlaceholder); // placeholder to satisfy DB NOT NULL/UNIQUE; overwritten on confirmation
        invoice.setReference(draftPlaceholder);       // placeholder to satisfy DB NOT NULL/UNIQUE; overwritten on confirmation
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(request.dueDate());
        invoice.setTemplateUsed(resolveInvoiceTemplate(tenantId, request));
        invoice.setCustomer(customer);
        invoice.setDescription(normalizeNullable(request.description()));
        invoice.setVatRate(request.vatRate());
        invoice.setAmount(calculateAmount(request.lineItems()));
        invoice.setStatus("DRAFT");

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

        // Add discounts if provided
        if (request.discounts() != null && !request.discounts().isEmpty()) {
            for (InvoiceDiscountRequest discountReq : request.discounts()) {
                InvoiceDiscount discount = new InvoiceDiscount();
                discount.setInvoice(saved);
                discount.setName(discountReq.name());
                discount.setDiscountType(discountReq.discountType());
                discount.setDiscountValue(discountReq.discountValue());
                invoiceDiscountRepository.save(discount);
            }
        }

        return toResponse(saved);
    }

    private InvoiceTemplate resolveInvoiceTemplate(Long tenantId, InvoiceCreateRequest request) {
        if (request.templateChoice() != null) {
            return request.templateChoice();
        }

        return companyRepository.findByTenantId(tenantId)
                .map(Company::getDefaultInvoiceTemplate)
                .orElse(InvoiceTemplate.CLASSIC);
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
        return invoiceRepository.findAllByTenantIdAndDeletedAtIsNullOrderByInvoiceDateDescIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse search(
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            Long customerId,
            Long exerciceId,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new BadRequestException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Size must be between 1 and 100");
        }

        Long tenantId = TenantContext.getRequiredTenantId();

        Long filterExerciceId = exerciceId;
        LocalDate effectiveFrom = fromDate;
        LocalDate effectiveTo = toDate;

        // If no filters are provided, default to the current active open Exercice if one exists
        if (filterExerciceId == null && fromDate == null && toDate == null) {
            Optional<Exercice> activeExercice = exerciceRepository.findAllByTenantIdOrderByStartDateDesc(tenantId).stream()
                    .filter(e -> e.getStatus() == ExerciceStatus.OPEN)
                    .findFirst();
            if (activeExercice.isPresent()) {
                filterExerciceId = activeExercice.get().getId();
            } else {
                // Fall back to calendar dates default if no open Exercice exists
                effectiveFrom = LocalDate.of(LocalDate.now().getYear(), 1, 1);
                effectiveTo = LocalDate.now();
            }
        }

        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        Specification<Invoice> baseSpec = buildSearchSpecification(tenantId, status, effectiveFrom, effectiveTo, customerId, filterExerciceId);

        // Push DRAFT-first + invoiceNumber asc sort to DB via CASE expression (correct across pages)
        Specification<Invoice> specWithOrder = (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                query.orderBy(
                    cb.asc(cb.selectCase()
                        .when(cb.equal(root.get("status"), "DRAFT"), 0)
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
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse lookupInvoice(String number) {
        if (number == null || number.isBlank()) {
            throw new BadRequestException("Invoice number must be provided");
        }

        Long tenantId = TenantContext.getRequiredTenantId();
        String trimmed = number.trim();

        // 1. Try search by formatted number
        Optional<Invoice> invoiceOpt = invoiceRepository.findByFormattedNumberAndTenantIdAndDeletedAtIsNull(trimmed, tenantId);

        // 2. If not found, try parsing as Long and search by raw invoiceNumber
        if (invoiceOpt.isEmpty()) {
            try {
                Long rawNum = Long.parseLong(trimmed);
                invoiceOpt = invoiceRepository.findByInvoiceNumberAndTenantIdAndDeletedAtIsNull(rawNum, tenantId);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // 3. Fallback: try stripping hash sign '#' and parse
        if (invoiceOpt.isEmpty() && trimmed.startsWith("#")) {
            try {
                Long rawNum = Long.parseLong(trimmed.substring(1));
                invoiceOpt = invoiceRepository.findByInvoiceNumberAndTenantIdAndDeletedAtIsNull(rawNum, tenantId);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        return invoiceOpt.map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with number: " + number));
    }

    @Transactional
    public InvoiceResponse update(Long id, InvoiceUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.isLocked()) {
            throw new BadRequestException("This invoice is locked and cannot be modified.");
        }

        if (invoice.getExercice() != null && invoice.getExercice().getStatus() == ExerciceStatus.CLOSED) {
            throw new BadRequestException("This invoice belongs to a closed fiscal year (exercice) and cannot be modified.");
        }

        if (request.invoiceDate() != null) {
            Exercice newExercice = exerciceRepository.findExerciceForDate(tenantId, request.invoiceDate())
                    .orElseThrow(() -> new BadRequestException("No fiscal year (exercice) defined for the date " + request.invoiceDate() + ". Please open one first."));
            if (newExercice.getStatus() == ExerciceStatus.CLOSED) {
                throw new BadRequestException("The fiscal year (exercice) " + newExercice.getName() + " is closed. Cannot assign invoices to it.");
            }
            invoice.setExercice(newExercice);
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
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.isLocked()) {
            throw new BadRequestException("This invoice is locked and cannot be deleted.");
        }

        if (invoice.getExercice() != null && invoice.getExercice().getStatus() == ExerciceStatus.CLOSED) {
            throw new BadRequestException("This invoice belongs to a closed fiscal year (exercice) and cannot be deleted.");
        }

        if (!"DRAFT".equals(invoice.getStatus()) && !"CANCELLED".equals(invoice.getStatus())) {
            throw new ConflictException("Only draft or cancelled invoices can be deleted");
        }

        Long officialNumber = invoice.getInvoiceNumber();
        invoice.setDeletedInvoiceNumber(officialNumber != null && officialNumber > 0 ? officialNumber : null);
        invoice.setInvoiceNumber(generateNextDeletedInvoiceNumber(tenantId));

        // Replace visible identifiers so unique constraints do not block future reuse.
        String deletedMarker = "DELETED-" + UUID.randomUUID();
        invoice.setFormattedNumber(deletedMarker);
        invoice.setReference(deletedMarker);
        invoice.setDeletedAt(Instant.now());
    }

    @Transactional
    public InvoiceResponse changeStatus(Long id, InvoiceStatusUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.isLocked()) {
            throw new BadRequestException("This invoice is locked and status cannot be changed.");
        }

        if (invoice.getExercice() != null && invoice.getExercice().getStatus() == ExerciceStatus.CLOSED) {
            throw new BadRequestException("This invoice belongs to a closed fiscal year (exercice) and cannot be modified.");
        }

        String oldStatus = invoice.getStatus();
        String targetStatus = request.status().trim().toUpperCase();
        if (oldStatus.equals(targetStatus)) {
            return toResponse(invoice);
        }

        InvoiceStatus oldStatusMeta = statusRepository.findByNameAndTenantId(oldStatus, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Status '" + oldStatus + "' not found"));
        InvoiceStatus targetStatusMeta = statusRepository.findByNameAndTenantId(targetStatus, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Status '" + targetStatus + "' not found"));

        if (!targetStatusMeta.isActive()) {
            throw new BadRequestException("The target status '" + targetStatus + "' is inactive and cannot be transitioned to.");
        }

        validateStatusTransition(oldStatusMeta.getCategory(), targetStatusMeta.getCategory());

        // When leaving DRAFT: validate date ordering and assign the official invoice number.
        if (oldStatusMeta.getCategory() == InvoiceStatusCategory.DRAFT && targetStatusMeta.getCategory() != InvoiceStatusCategory.DRAFT) {
            if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber() < 0) {
                Long exerciceId = requireExerciceId(invoice);
                validateInvoiceDateOrdering(invoice.getInvoiceDate(), tenantId, exerciceId);
                assignOfficialNumber(invoice, tenantId);
            }
        }

        invoice.setStatus(targetStatus);
        saveStatusChangeLog(invoice, oldStatus, targetStatus);

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceStatusChangeLogResponse> findStatusHistory(Long invoiceId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        return statusChangeLogRepository.findByInvoice_IdAndTenantIdOrderByChangedAtDesc(invoiceId, tenantId)
                .stream()
                .map(this::toStatusChangeLogResponse)
                .toList();
    }

    private void validateInvoiceDateOrdering(LocalDate invoiceDate, Long tenantId, Long exerciceId) {
        invoiceRepository.findMaxOfficialInvoiceDateByTenantIdAndExerciceId(tenantId, exerciceId).ifPresent(maxDate -> {
            if (invoiceDate.isBefore(maxDate)) {
                throw new ConflictException(
                    "Invoice date " + invoiceDate + " is before the most recent official invoice date " + maxDate +
                    ". You cannot backdate a confirmed invoice."
                );
            }
        });
    }

    private void assignOfficialNumber(Invoice invoice, Long tenantId) {
        Long exerciceId = requireExerciceId(invoice);
        Long maxOfficialNumber = invoiceRepository.getMaxOfficialInvoiceNumberForExercice(tenantId, exerciceId);
        Long nextNumber = maxOfficialNumber + 1;

        Optional<Invoice> latestDeletedOfficialInvoice =
                invoiceRepository.findTopByTenantIdAndExercice_IdAndDeletedAtIsNotNullAndDeletedInvoiceNumberIsNotNullOrderByDeletedAtDesc(
                        tenantId,
                        exerciceId
                );
        if (latestDeletedOfficialInvoice.isPresent()) {
            Long deletedNumber = latestDeletedOfficialInvoice.get().getDeletedInvoiceNumber();
            if (deletedNumber != null && deletedNumber > maxOfficialNumber) {
                nextNumber = deletedNumber;
            }
        }

        String formatted = generateFormattedNumber(nextNumber, invoice.getInvoiceDate().getYear());
        invoice.setInvoiceNumber(nextNumber);
        invoice.setFormattedNumber(formatted);
        invoice.setReference(formatted);
    }

    private Long requireExerciceId(Invoice invoice) {
        if (invoice.getExercice() == null || invoice.getExercice().getId() == null) {
            throw new BadRequestException("Invoice must belong to an exercice");
        }
        return invoice.getExercice().getId();
    }

    private Long generateNextDraftNumber(Long tenantId) {
        Long minNumber = invoiceRepository.getMinInvoiceNumberForTenant(tenantId);
        return minNumber > 0 ? -1L : minNumber - 1L;
    }

    private Long generateNextDeletedInvoiceNumber(Long tenantId) {
        Long minNumber = invoiceRepository.getMinInvoiceNumberForTenantIncludingDeleted(tenantId);
        return minNumber > 0 ? -1L : minNumber - 1L;
    }

    private String generateDraftPlaceholder() {
        return "DRAFT-" + UUID.randomUUID();
    }

    private void saveStatusChangeLog(Invoice invoice, String oldStatus, String newStatus) {
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

    private String resolvePrettyName(String createdBy) {
        if (createdBy == null || createdBy.isBlank() || "system".equalsIgnoreCase(createdBy)) {
            return "System";
        }
        return appUserRepository.findByEmail(createdBy.toLowerCase().trim())
                .map(u -> {
                    String first = u.getFirstName();
                    String last = u.getLastName();
                    if (first != null && !first.isBlank() && last != null && !last.isBlank()) {
                        String initial = last.substring(0, 1).toUpperCase();
                        String formattedFirst = first.substring(0, 1).toUpperCase() + (first.length() > 1 ? first.substring(1) : "");
                        return initial + ". " + formattedFirst;
                    }
                    if (first != null && !first.isBlank()) {
                        return first;
                    }
                    if (last != null && !last.isBlank()) {
                        return last;
                    }
                    return createdBy;
                })
                .orElse(createdBy);
    }

    private InvoiceStatusChangeLogResponse toStatusChangeLogResponse(InvoiceStatusChangeLog log) {
        return new InvoiceStatusChangeLogResponse(
                log.getChangedAt(),
                resolvePrettyName(log.getCreatedBy()),
                log.getOldStatus(),
                log.getNewStatus()
        );
    }

    @Transactional
    public InvoicePaymentResponse addPayment(Long invoiceId, InvoicePaymentRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        BigDecimal totalAmount = calculateTotalAmount(invoice);
        BigDecimal paidAmount = calculatePaidAmount(paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoice.getId())
                .stream().map(this::toPaymentResponse).toList());
        BigDecimal remainingAmount = calculateRemainingAmount(totalAmount, paidAmount);

        if (invoice.isLocked() && remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This invoice is locked and already fully paid. Cannot record new payments.");
        }

        InvoicePayment payment = new InvoicePayment();
        payment.setInvoice(invoice);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentReference(normalizeNullable(request.paymentReference()));
        payment.setPaymentDate(request.paymentDate());
        payment.setPaidAmount(request.paidAmount());
        payment.setBankName(normalizeNullable(request.bankName()));

        InvoicePayment saved = paymentRepository.save(payment);
        return toPaymentResponse(saved);
    }

    @Transactional
    public void removePayment(Long invoiceId, Long paymentId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.isLocked()) {
            throw new BadRequestException("This invoice is locked and payments cannot be deleted.");
        }

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

        List<InvoiceDiscountResponse> discounts = invoiceDiscountRepository.findByInvoiceIdOrderByIdAsc(invoice.getId())
                .stream()
                .map(d -> new InvoiceDiscountResponse(
                        d.getId(),
                        d.getName(),
                        d.getDiscountType(),
                        d.getDiscountValue()
                ))
                .toList();

        BigDecimal totalGrossAmount = invoice.getAmount();
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        for (InvoiceDiscountResponse d : discounts) {
            if (d.discountType() == DiscountType.PERCENTAGE) {
                BigDecimal pctAmt = totalGrossAmount.multiply(d.discountValue()).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
                totalDiscountAmount = totalDiscountAmount.add(pctAmt);
            } else {
                totalDiscountAmount = totalDiscountAmount.add(d.discountValue());
            }
        }
        totalDiscountAmount = totalDiscountAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNetAmount = totalGrossAmount.subtract(totalDiscountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal vatAmount = totalNetAmount.multiply(invoice.getVatRate()).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = totalNetAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal paidAmount = calculatePaidAmount(payments);
        BigDecimal remainingAmount = calculateRemainingAmount(totalAmount, paidAmount);

        // Fetch company information for the current tenant
        InvoiceCompanyResponse companyResponse = companyRepository.findByTenantId(invoice.getTenantId())
                .map(this::toCompanyResponse)
                .orElse(null);

        InvoiceStatusResponse statusDetails = statusRepository.findByNameAndTenantId(invoice.getStatus(), invoice.getTenantId())
                .map(statusService::toResponse)
                .orElse(null);

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getFormattedNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                resolveInvoiceTemplate(invoice.getTemplateUsed()),
                companyResponse,
                toCustomerResponse(invoice.getCustomer()),
                invoice.getDescription(),
                invoice.getVatRate(),
                paidAmount,
                remainingAmount,
                resolvePaymentStatus(totalAmount, remainingAmount),
                invoice.getStatus(),
                statusDetails,
                invoice.isLocked(),
                lineItems,
                payments,
                discounts,
                totalGrossAmount,
                totalDiscountAmount,
                totalNetAmount,
                invoice.getTenantId(),
                invoice.getCreatedAt(),
                invoice.getExercice() != null ? invoice.getExercice().getId() : null,
                invoice.getExercice() != null ? invoice.getExercice().getName() : null
        );
    }

    private InvoiceTemplate resolveInvoiceTemplate(InvoiceTemplate template) {
        return template != null ? template : InvoiceTemplate.CLASSIC;
    }

    private Specification<Invoice> buildSearchSpecification(
            Long tenantId,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            Long customerId,
            Long exerciceId
    ) {
        Specification<Invoice> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        spec = spec.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
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
        if (exerciceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("exercice").get("id"), exerciceId));
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

    private InvoiceCompanyResponse toCompanyResponse(Company company) {
        return new InvoiceCompanyResponse(
                company.getId(),
                company.getName(),
                company.getEmail(),
                company.getPhone(),
                company.getAddress(),
                company.getTaxId(),
                company.getRegistreCommerce(),
                company.getLogo(),
                company.getWebsite(),
                company.getCurrency(),
                company.getLanguage(),
                company.getDefaultVatRate(),
                company.getPaymentTermsInDays()
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
                payment.getPaidAmount(),
                payment.getBankName()
        );
    }

    @Transactional(readOnly = true)
    public List<com.elmeftouhi.facturesimple.invoice.dto.TenantPaymentResponse> findAllPayments() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return paymentRepository.findAllByTenantIdOrderByPaymentDateDescIdDesc(tenantId)
                .stream()
                .map(this::toTenantPaymentResponse)
                .toList();
    }

    private com.elmeftouhi.facturesimple.invoice.dto.TenantPaymentResponse toTenantPaymentResponse(InvoicePayment payment) {
        Invoice invoice = payment.getInvoice();
        String customerName = (invoice != null && invoice.getCustomer() != null) ? invoice.getCustomer().getName() : "Inline Customer";
        String invoiceNum = (invoice != null) ? (invoice.getFormattedNumber() != null ? invoice.getFormattedNumber() : "#" + invoice.getInvoiceNumber()) : "-";
        Long invoiceId = (invoice != null) ? invoice.getId() : null;

        return new com.elmeftouhi.facturesimple.invoice.dto.TenantPaymentResponse(
                payment.getId(),
                payment.getPaymentMethod(),
                payment.getPaymentReference(),
                payment.getPaymentDate(),
                payment.getPaidAmount(),
                invoiceId,
                invoiceNum,
                customerName,
                payment.getBankName()
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

    @Transactional
    public InvoiceResponse toggleLock(Long id, boolean locked) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.setLocked(locked);
        return toResponse(invoice);
    }

    private BigDecimal calculateTotalAmount(Invoice invoice) {
        List<InvoiceDiscount> discounts = invoiceDiscountRepository.findByInvoiceIdOrderByIdAsc(invoice.getId());
        BigDecimal totalGrossAmount = invoice.getAmount();
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        for (InvoiceDiscount d : discounts) {
            if (d.getDiscountType() == DiscountType.PERCENTAGE) {
                BigDecimal pctAmt = totalGrossAmount.multiply(d.getDiscountValue()).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
                totalDiscountAmount = totalDiscountAmount.add(pctAmt);
            } else {
                totalDiscountAmount = totalDiscountAmount.add(d.getDiscountValue());
            }
        }
        totalDiscountAmount = totalDiscountAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNetAmount = totalGrossAmount.subtract(totalDiscountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal vatAmount = totalNetAmount.multiply(invoice.getVatRate()).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
        return totalNetAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateStatusTransition(InvoiceStatusCategory current, InvoiceStatusCategory target) {
        boolean valid = switch (current) {
            case DRAFT -> target == InvoiceStatusCategory.CONFIRMED || target == InvoiceStatusCategory.CANCELLED;
            case CONFIRMED -> target == InvoiceStatusCategory.CONFIRMED || target == InvoiceStatusCategory.CANCELLED || target == InvoiceStatusCategory.CLOSED || target == InvoiceStatusCategory.DRAFT;
            case CANCELLED -> target == InvoiceStatusCategory.CLOSED;
            case CLOSED -> false;
        };

        if (!valid) {
            throw new ConflictException("Invalid invoice status category transition from " + current + " to " + target);
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

