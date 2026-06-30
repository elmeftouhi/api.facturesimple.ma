package com.elmeftouhi.facturesimple.company;

import com.elmeftouhi.facturesimple.company.dto.CompanyBankRequest;
import com.elmeftouhi.facturesimple.company.dto.CompanyBankResponse;
import com.elmeftouhi.facturesimple.company.dto.CompanyCreateRequest;
import com.elmeftouhi.facturesimple.company.dto.CompanyResponse;
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
@RequestMapping("/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // Company Endpoints

    /**
     * Get company information for the current tenant.
     */
    @GetMapping
    public CompanyResponse getCompany() {
        return companyService.getCompany();
    }

    /**
     * Create company information for the current tenant.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        return companyService.createCompany(request);
    }

    /**
     * Update company information for the current tenant.
     */
    @PutMapping
    public CompanyResponse updateCompany(@Valid @RequestBody CompanyCreateRequest request) {
        return companyService.updateCompany(request);
    }

    /**
     * Delete company information for the current tenant.
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany() {
        companyService.deleteCompany();
    }

    // Bank Endpoints

    /**
     * List banks for the current tenant company.
     */
    @GetMapping("/banks")
    public List<CompanyBankResponse> getBanks() {
        return companyService.getBanks();
    }

    /**
     * Add a new bank to the company.
     */
    @PostMapping("/banks")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyBankResponse addBank(@Valid @RequestBody CompanyBankRequest request) {
        return companyService.addBank(request);
    }

    /**
     * Update bank information.
     */
    @PutMapping("/banks/{bankId}")
    public CompanyBankResponse updateBank(
            @PathVariable Long bankId,
            @Valid @RequestBody CompanyBankRequest request
    ) {
        return companyService.updateBank(bankId, request);
    }

    /**
     * Set a bank as the default bank for the company.
     */
    @PutMapping("/banks/{bankId}/default")
    public CompanyBankResponse setDefaultBank(@PathVariable Long bankId) {
        return companyService.setDefaultBank(bankId);
    }

    /**
     * Delete a bank.
     */
    @DeleteMapping("/banks/{bankId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBank(@PathVariable Long bankId) {
        companyService.deleteBank(bankId);
    }
}

