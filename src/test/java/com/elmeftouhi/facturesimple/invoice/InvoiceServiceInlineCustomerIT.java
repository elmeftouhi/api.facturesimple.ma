package com.elmeftouhi.facturesimple.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.elmeftouhi.facturesimple.customer.Customer;
import com.elmeftouhi.facturesimple.customer.CustomerRepository;
import com.elmeftouhi.facturesimple.customer.category.CustomerCategory;
import com.elmeftouhi.facturesimple.customer.category.CustomerCategoryRepository;
import com.elmeftouhi.facturesimple.customer.dto.CustomerCreateRequest;
import com.elmeftouhi.facturesimple.exercice.Exercice;
import com.elmeftouhi.facturesimple.exercice.ExerciceRepository;
import com.elmeftouhi.facturesimple.exercice.ExerciceStatus;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCreateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceLineItemRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePageResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusChangeLogResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusUpdateRequest;
import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InvoiceServiceInlineCustomerIT {

    private static final Long TENANT_ID = 100L;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerCategoryRepository customerCategoryRepository;

    @Autowired
    private InvoiceStatusChangeLogRepository statusChangeLogRepository;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @BeforeEach
    void setUpTenant() {
        TenantContext.setTenantId(TENANT_ID);
        ensureOpenExerciceExists();
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void ensureOpenExerciceExists() {
        LocalDate today = LocalDate.now();
        List<Exercice> matching = exerciceRepository.findAllByTenantIdOrderByStartDateDesc(TENANT_ID).stream()
                .filter(exercice -> !exercice.getStartDate().isAfter(today) && !exercice.getEndDate().isBefore(today))
                .toList();

        if (matching.isEmpty()) {
            Exercice exercice = new Exercice();
            exercice.setName("FY-" + today.getYear());
            exercice.setStartDate(LocalDate.of(today.getYear(), 1, 1));
            exercice.setEndDate(LocalDate.of(today.getYear(), 12, 31));
            exercice.setStatus(ExerciceStatus.OPEN);
            exerciceRepository.save(exercice);
            return;
        }

        Exercice exercice = matching.get(0);
        if (exercice.getStatus() != ExerciceStatus.OPEN) {
            exercice.setStatus(ExerciceStatus.OPEN);
            exerciceRepository.save(exercice);
        }
    }

    @Test
    void createWithNewCustomerSavesCustomerAndInvoice() {
        CustomerCategory category = createCategory();

        InvoiceCreateRequest request = new InvoiceCreateRequest(
                null,
                new CustomerCreateRequest("Inline Customer", "inline@example.com", "123456", "Address", "TAX-1", category.getId()),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Inline customer invoice",
                new BigDecimal("20.00"),
                InvoiceTemplate.MODERN,
                List.of(new InvoiceLineItemRequest("ITEM-1", "Service", new BigDecimal("1.00"), new BigDecimal("100.00"))),
                List.of()
        );

        InvoiceResponse response = invoiceService.create(request);

        assertThat(response.customer()).isNotNull();
        assertThat(response.customer().id()).isNotNull();
        assertThat(response.templateUsed()).isEqualTo(InvoiceTemplate.MODERN);
        assertThat(customerRepository.findByIdAndTenantId(response.customer().id(), TENANT_ID)).isPresent();
        assertThat(invoiceRepository.findByIdAndTenantId(response.id(), TENANT_ID)).isPresent();
    }

    @Test
    void createWithCustomerIdAndNewCustomerThrowsBadRequest() {
        CustomerCategory category = createCategory();
        Customer existing = new Customer();
        existing.setName("Existing Customer");
        existing.setCategory(category);
        Customer savedCustomer = customerRepository.save(existing);

        InvoiceCreateRequest request = new InvoiceCreateRequest(
                savedCustomer.getId(),
                new CustomerCreateRequest("Inline Customer", "inline@example.com", null, null, null, category.getId()),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Invalid request",
                new BigDecimal("20.00"),
                null,
                List.of(new InvoiceLineItemRequest("ITEM-1", "Service", new BigDecimal("1.00"), new BigDecimal("100.00"))),
                List.of()
        );

        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exactly one of customerId or newCustomer");
    }

    @Test
    void createRollsBackInlineCustomerWhenInvoiceSaveFails() {
        CustomerCategory category = createCategory();
        int customersBefore = customerRepository.findAllByTenantIdOrderByIdDesc(TENANT_ID).size();
        int invoicesBefore = invoiceRepository.findAllByTenantIdOrderByInvoiceDateDescIdDesc(TENANT_ID).size();

        InvoiceCreateRequest request = new InvoiceCreateRequest(
                null,
                new CustomerCreateRequest("Rollback Customer", "rollback@example.com", null, null, null, category.getId()),
                null,
                LocalDate.now().plusDays(10),
                "Will fail",
                new BigDecimal("20.00"),
                null,
                List.of(new InvoiceLineItemRequest("ITEM-1", "Service", new BigDecimal("1.00"), new BigDecimal("100.00"))),
                List.of()
        );

        assertThatThrownBy(() -> invoiceService.create(request)).isInstanceOf(RuntimeException.class);

        assertThat(customerRepository.findAllByTenantIdOrderByIdDesc(TENANT_ID)).hasSize(customersBefore);
        assertThat(invoiceRepository.findAllByTenantIdOrderByInvoiceDateDescIdDesc(TENANT_ID)).hasSize(invoicesBefore);
    }

    @Test
    void createDraftInvoiceHasNoOfficialNumber() {
        InvoiceResponse draft = createDraftInvoice();
        assertThat(draft.invoiceNumber()).isNotNull();
        assertThat(draft.invoiceNumber()).isLessThan(0);
        assertThat(draft.formattedNumber()).startsWith("DRAFT-");
        assertThat(draft.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(draft.templateUsed()).isEqualTo(InvoiceTemplate.CLASSIC);
    }

    @Test
    void changeStatusFromDraftAssignsOfficialNumber() {
        InvoiceResponse draft = createDraftInvoice();
        assertThat(draft.invoiceNumber()).isLessThan(0);

        InvoiceResponse printed = invoiceService.changeStatus(draft.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));
        assertThat(printed.invoiceNumber()).isNotNull();
        assertThat(printed.invoiceNumber()).isGreaterThan(0);
        assertThat(printed.formattedNumber()).isNotNull();
    }

    @Test
    void deletedDraftDoesNotGapOfficialNumberSequence() {
        InvoiceResponse draft1 = createDraftInvoice();
        InvoiceResponse draft2 = createDraftInvoice();

        // Confirm draft1 → get an official number
        InvoiceResponse printed1 = invoiceService.changeStatus(draft1.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));
        Long firstOfficialNumber = printed1.invoiceNumber();

        // Delete draft2 without confirming it — must not create a gap
        invoiceService.delete(draft2.id());

        // Confirm a brand-new draft → should follow directly after firstOfficialNumber
        InvoiceResponse draft3 = createDraftInvoice();
        InvoiceResponse printed3 = invoiceService.changeStatus(draft3.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));
        assertThat(printed3.invoiceNumber()).isEqualTo(firstOfficialNumber + 1);
    }

    @Test
    void changeStatusRejectsBackdatedInvoice() {
        // First, confirm an invoice dated today
        InvoiceResponse draft1 = createDraftInvoiceWithDate(LocalDate.now());
        invoiceService.changeStatus(draft1.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));

        // Try to confirm a draft with a date before the most recent official invoice
        InvoiceResponse draft2 = createDraftInvoiceWithDate(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> invoiceService.changeStatus(draft2.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("backdate");
    }

    private InvoiceResponse createDraftInvoiceWithDate(LocalDate date) {
        CustomerCategory category = createCategory();
        return invoiceService.create(new InvoiceCreateRequest(
                null,
                new CustomerCreateRequest("Date Customer " + System.nanoTime(), "date@example.com", null, null, null, category.getId()),
                date,
                date.plusDays(10),
                "Date test invoice",
                new BigDecimal("20.00"),
                null,
                List.of(new InvoiceLineItemRequest("ITEM-D", "Service", new BigDecimal("1.00"), new BigDecimal("100.00"))),
                List.of()
        ));
    }

    @Test
    void changeStatusAllowsArchivedToFinal() {
        InvoiceResponse createdInvoice = createDraftInvoice();

        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));
        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.SOLD));
        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.ARCHIVED));

        InvoiceResponse finalInvoice = invoiceService.changeStatus(
                createdInvoice.id(),
                new InvoiceStatusUpdateRequest(InvoiceStatus.FINAL)
        );

        assertThat(finalInvoice.status()).isEqualTo(InvoiceStatus.FINAL);
    }

    @Test
    void changeStatusRejectsUpdatesWhenInvoiceIsFinal() {
        InvoiceResponse createdInvoice = createDraftInvoice();

        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));
        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.SOLD));
        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.ARCHIVED));
        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.FINAL));

        assertThatThrownBy(() -> invoiceService.changeStatus(
                createdInvoice.id(),
                new InvoiceStatusUpdateRequest(InvoiceStatus.DRAFT)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Invalid invoice status transition from FINAL to DRAFT");
    }

    @Test
    void changeStatusSavesAuditLogWithDateCreatorAndStatuses() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(55L, "audit-user", TENANT_ID, Set.of(TENANT_ID)),
                null,
                List.of()
        ));

        InvoiceResponse createdInvoice = createDraftInvoice();
        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));

        List<InvoiceStatusChangeLog> logs = statusChangeLogRepository
                .findByInvoice_IdAndTenantIdOrderByChangedAtDesc(createdInvoice.id(), TENANT_ID);

        assertThat(logs).hasSize(1);
        InvoiceStatusChangeLog log = logs.get(0);
        assertThat(log.getOldStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(log.getNewStatus()).isEqualTo(InvoiceStatus.PRINTED);
        assertThat(log.getCreatedBy()).isEqualTo("audit-user");
        assertThat(log.getChangedAt()).isNotNull();
    }

    @Test
    void findStatusHistoryReturnsNewestFirst() {
        InvoiceResponse createdInvoice = createDraftInvoice();

        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));
        invoiceService.changeStatus(createdInvoice.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.SOLD));

        List<InvoiceStatusChangeLogResponse> history = invoiceService.findStatusHistory(createdInvoice.id());

        assertThat(history).hasSize(2);
        assertThat(history.get(0).oldStatus()).isEqualTo(InvoiceStatus.PRINTED);
        assertThat(history.get(0).newStatus()).isEqualTo(InvoiceStatus.SOLD);
        assertThat(history.get(1).oldStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(history.get(1).newStatus()).isEqualTo(InvoiceStatus.PRINTED);
    }

    @Test
    void findByIdReturnsPaymentSummary() {
        InvoiceResponse createdInvoice = createDraftInvoice();

        invoiceService.addPayment(createdInvoice.id(), new InvoicePaymentRequest(
                PaymentMethod.CASH,
                "PAY-1",
                LocalDate.now(),
                new BigDecimal("40.00")
        ));

        InvoiceResponse reloadedInvoice = invoiceService.findById(createdInvoice.id());

        assertThat(reloadedInvoice.paidAmount()).isEqualByComparingTo("40.00");
        assertThat(reloadedInvoice.remainingAmount()).isEqualByComparingTo("60.00");
        assertThat(reloadedInvoice.paymentStatus()).isEqualTo(InvoicePaymentStatus.PARTIAL);
    }

    @Test
    void searchFiltersByStatusAndPaginates() {
        CustomerCategory category = createCategory();
        Customer customer = new Customer();
        customer.setName("Search Customer " + System.nanoTime());
        customer.setCategory(category);
        Customer savedCustomer = customerRepository.save(customer);

        InvoiceResponse invoice1 = invoiceService.create(new InvoiceCreateRequest(
                savedCustomer.getId(),
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                "Search invoice 1",
                new BigDecimal("20.00"),
                null,
                List.of(new InvoiceLineItemRequest("ITEM-S1", "Service", new BigDecimal("1.00"), new BigDecimal("100.00"))),
                List.of()
        ));
        InvoiceResponse invoice2 = invoiceService.create(new InvoiceCreateRequest(
                savedCustomer.getId(),
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                "Search invoice 2",
                new BigDecimal("20.00"),
                null,
                List.of(new InvoiceLineItemRequest("ITEM-S2", "Service", new BigDecimal("1.00"), new BigDecimal("100.00"))),
                List.of()
        ));

        invoiceService.changeStatus(invoice1.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));
        invoiceService.changeStatus(invoice2.id(), new InvoiceStatusUpdateRequest(InvoiceStatus.PRINTED));

        InvoicePageResponse firstPage = invoiceService.search(InvoiceStatus.PRINTED, null, null, savedCustomer.getId(), null, 0, 1);
        InvoicePageResponse secondPage = invoiceService.search(InvoiceStatus.PRINTED, null, null, savedCustomer.getId(), null, 1, 1);

        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();

        assertThat(secondPage.totalElements()).isEqualTo(2);
        assertThat(secondPage.content()).hasSize(1);
    }

    private InvoiceResponse createDraftInvoice() {
        CustomerCategory category = createCategory();
        return invoiceService.create(new InvoiceCreateRequest(
                null,
                new CustomerCreateRequest("Status Customer " + System.nanoTime(), "status@example.com", null, null, null, category.getId()),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Status flow invoice",
                new BigDecimal("20.00"),
                null,
                List.of(new InvoiceLineItemRequest("ITEM-STATUS", "Service", new BigDecimal("1.00"), new BigDecimal("100.00"))),
                List.of()
        ));
    }

    private CustomerCategory createCategory() {
        CustomerCategory category = new CustomerCategory();
        category.setName("Category-" + System.nanoTime());
        category.setDescription("test");
        category.setDefaultCategory(false);
        return customerCategoryRepository.save(category);
    }
}

