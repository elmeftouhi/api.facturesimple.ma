 package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.invoice.dto.InvoiceCreateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePageResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoicePaymentResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusChangeLogResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusUpdateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceUpdateRequest;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceCreateRequest request) {
        return invoiceService.create(request);
    }

    @GetMapping
    public InvoicePageResponse findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return invoiceService.search(parseStatus(status), fromDate, toDate, customerId, page, size);
    }

    private InvoiceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return InvoiceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            String allowed = Arrays.stream(InvoiceStatus.values())
                    .map(Enum::name)
                    .toList()
                    .toString();
            throw new BadRequestException("Invalid status '" + status + "'. Allowed values: " + allowed);
        }
    }

    @GetMapping("/{id}")
    public InvoiceResponse findById(@PathVariable Long id) {
        return invoiceService.findById(id);
    }

    @PutMapping("/{id}")
    public InvoiceResponse update(@PathVariable Long id, @Valid @RequestBody InvoiceUpdateRequest request) {
        return invoiceService.update(id, request);
    }

    @PutMapping("/{id}/status")
    public InvoiceResponse changeStatus(@PathVariable Long id, @Valid @RequestBody InvoiceStatusUpdateRequest request) {
        return invoiceService.changeStatus(id, request);
    }

    @GetMapping("/{id}/status-history")
    public List<InvoiceStatusChangeLogResponse> findStatusHistory(@PathVariable Long id) {
        return invoiceService.findStatusHistory(id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoicePaymentResponse addPayment(@PathVariable Long id, @Valid @RequestBody InvoicePaymentRequest request) {
        return invoiceService.addPayment(id, request);
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePayment(@PathVariable Long id, @PathVariable Long paymentId) {
        invoiceService.removePayment(id, paymentId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        invoiceService.delete(id);
    }
}

