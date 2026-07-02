package com.elmeftouhi.facturesimple.invoice;

import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusSettingsCreateRequest;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusResponse;
import com.elmeftouhi.facturesimple.invoice.dto.InvoiceStatusSettingsUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/invoice-statuses")
@RequiredArgsConstructor
public class InvoiceStatusController {

    private final InvoiceStatusService statusService;

    @GetMapping
    public List<InvoiceStatusResponse> findAll() {
        return statusService.findAll();
    }

    @GetMapping("/active")
    public List<InvoiceStatusResponse> findAllActive() {
        return statusService.findAllActive();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceStatusResponse create(@Valid @RequestBody InvoiceStatusSettingsCreateRequest request) {
        return statusService.create(request);
    }

    @PutMapping("/{id}")
    public InvoiceStatusResponse update(@PathVariable Long id, @Valid @RequestBody InvoiceStatusSettingsUpdateRequest request) {
        return statusService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        statusService.delete(id);
    }
}
