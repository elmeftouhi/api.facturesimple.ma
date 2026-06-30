package com.elmeftouhi.facturesimple.company;

import com.elmeftouhi.facturesimple.company.dto.CompanyBankRequest;
import com.elmeftouhi.facturesimple.company.dto.CompanyBankResponse;
import com.elmeftouhi.facturesimple.company.dto.CompanyCreateRequest;
import com.elmeftouhi.facturesimple.company.dto.CompanyResponse;
import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyBankRepository companyBankRepository;

    // Company CRUD Operations

    /**
     * Get company information for the current tenant.
     */
    public CompanyResponse getCompany() {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));
        return mapToCompanyResponse(company);
    }

    /**
     * Create company information for the current tenant.
     * One company per tenant is enforced at the database level.
     */
    public CompanyResponse createCompany(CompanyCreateRequest request) {
        Long tenantId = TenantContext.getTenantId();

        // Check if company already exists for this tenant
        if (companyRepository.findByTenantId(tenantId).isPresent()) {
            throw new BadRequestException("Company information already exists for this tenant. Use update instead.");
        }

        Company company = new Company();
        updateCompanyFields(company, request);
        // tenantId will be automatically assigned by TenantEntityListener

        Company saved = companyRepository.save(company);
        return mapToCompanyResponse(saved);
    }

    /**
     * Update company information for the current tenant.
     */
    public CompanyResponse updateCompany(CompanyCreateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));

        updateCompanyFields(company, request);
        company.setUpdatedAt(java.time.Instant.now());

        Company saved = companyRepository.save(company);
        return mapToCompanyResponse(saved);
    }

    /**
     * Delete company information for the current tenant.
     */
    public void deleteCompany() {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));
        companyRepository.delete(company);
    }

    // Bank Management Operations

    /**
     * List banks for the current tenant company.
     */
    public List<CompanyBankResponse> getBanks() {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));

        return companyBankRepository.findByCompanyId(company.getId())
                .stream()
                .map(this::mapToCompanyBankResponse)
                .toList();
    }

    /**
     * Add a new bank to the company.
     */
    public CompanyBankResponse addBank(CompanyBankRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));

        // If this is the first bank or marked as default, ensure only one default
        if (Boolean.TRUE.equals(request.isDefault())) {
            companyBankRepository.findByCompanyIdAndIsDefaultTrue(company.getId())
                    .ifPresent(defaultBank -> {
                        defaultBank.setIsDefault(false);
                        companyBankRepository.save(defaultBank);
                    });
        }

        CompanyBank bank = new CompanyBank();
        bank.setCompany(company);
        bank.setBankName(request.bankName());
        bank.setAccountNumber(request.accountNumber());
        bank.setSwiftCode(request.swiftCode());
        bank.setIban(request.iban());
        bank.setIsDefault(Boolean.TRUE.equals(request.isDefault()) || company.getBanks().isEmpty());

        CompanyBank saved = companyBankRepository.save(bank);
        company.getBanks().add(saved);

        return mapToCompanyBankResponse(saved);
    }

    /**
     * Update bank information.
     */
    public CompanyBankResponse updateBank(Long bankId, CompanyBankRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));

        CompanyBank bank = companyBankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found"));

        // Verify bank belongs to the current tenant's company
        if (!bank.getCompany().getId().equals(company.getId())) {
            throw new BadRequestException("Bank does not belong to this company");
        }

        // If marking as default, unset other defaults
        if (Boolean.TRUE.equals(request.isDefault()) && !bank.getIsDefault()) {
            companyBankRepository.findByCompanyIdAndIsDefaultTrue(company.getId())
                    .ifPresent(defaultBank -> {
                        defaultBank.setIsDefault(false);
                        companyBankRepository.save(defaultBank);
                    });
        }

        bank.setBankName(request.bankName());
        bank.setAccountNumber(request.accountNumber());
        bank.setSwiftCode(request.swiftCode());
        bank.setIban(request.iban());
        bank.setIsDefault(Boolean.TRUE.equals(request.isDefault()));

        CompanyBank saved = companyBankRepository.save(bank);
        return mapToCompanyBankResponse(saved);
    }

    /**
     * Set a bank as the default bank for the company.
     */
    public CompanyBankResponse setDefaultBank(Long bankId) {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));

        CompanyBank bank = companyBankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found"));

        // Verify bank belongs to the current tenant's company
        if (!bank.getCompany().getId().equals(company.getId())) {
            throw new BadRequestException("Bank does not belong to this company");
        }

        // Unset current default
        companyBankRepository.findByCompanyIdAndIsDefaultTrue(company.getId())
                .ifPresent(defaultBank -> {
                    if (!defaultBank.getId().equals(bankId)) {
                        defaultBank.setIsDefault(false);
                        companyBankRepository.save(defaultBank);
                    }
                });

        bank.setIsDefault(true);
        CompanyBank saved = companyBankRepository.save(bank);
        return mapToCompanyBankResponse(saved);
    }

    /**
     * Delete a bank.
     */
    public void deleteBank(Long bankId) {
        Long tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company information not found for tenant"));

        CompanyBank bank = companyBankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found"));

        // Verify bank belongs to the current tenant's company
        if (!bank.getCompany().getId().equals(company.getId())) {
            throw new BadRequestException("Bank does not belong to this company");
        }

        companyBankRepository.delete(bank);
    }

    // Utility Methods

    private void updateCompanyFields(Company company, CompanyCreateRequest request) {
        company.setName(request.name());
        company.setEmail(request.email());
        company.setPhone(request.phone());
        company.setAddress(request.address());
        company.setTaxId(request.taxId());
        company.setRegistreCommerce(request.registreCommerce());
        company.setLogo(request.logo());
        company.setWebsite(request.website());
        company.setCurrency(request.currency() != null ? request.currency() : "MAD");
        company.setDefaultVatRate(request.defaultVatRate());
        company.setPaymentTermsInDays(request.paymentTermsInDays());
        company.setDescription(request.description());
    }

    private CompanyResponse mapToCompanyResponse(Company company) {
        return new CompanyResponse(
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
                company.getDefaultVatRate(),
                company.getPaymentTermsInDays(),
                company.getDescription(),
                company.getBanks().stream().map(this::mapToCompanyBankResponse).toList(),
                company.getTenantId(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }

    private CompanyBankResponse mapToCompanyBankResponse(CompanyBank bank) {
        return new CompanyBankResponse(
                bank.getId(),
                bank.getBankName(),
                bank.getAccountNumber(),
                bank.getSwiftCode(),
                bank.getIban(),
                bank.getIsDefault(),
                bank.getCreatedAt()
        );
    }
}

