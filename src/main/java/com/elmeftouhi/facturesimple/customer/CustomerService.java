package com.elmeftouhi.facturesimple.customer;

import com.elmeftouhi.facturesimple.customer.category.CustomerCategory;
import com.elmeftouhi.facturesimple.customer.category.CustomerCategoryRepository;
import com.elmeftouhi.facturesimple.customer.dto.CustomerCreateRequest;
import com.elmeftouhi.facturesimple.customer.dto.CustomerResponse;
import com.elmeftouhi.facturesimple.customer.dto.CustomerUpdateRequest;
import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerCategoryRepository customerCategoryRepository;

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        if (customerRepository.existsByNameIgnoreCaseAndTenantId(request.name(), tenantId)) {
            throw new ConflictException("Customer name already exists in this tenant");
        }

        Customer customer = new Customer();
        customer.setName(request.name().trim());
        customer.setEmail(normalizeNullable(request.email()));
        customer.setPhone(normalizeNullable(request.phone()));
        customer.setAddress(normalizeNullable(request.address()));
        customer.setTaxId(normalizeNullable(request.taxId()));
        customer.setCategory(resolveCategoryOrDefault(request.categoryId(), tenantId));

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return customerRepository.findAllByTenantIdOrderByIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw new BadRequestException("Customer name cannot be blank");
            }

            if (!customer.getName().equalsIgnoreCase(newName)
                    && customerRepository.existsByNameIgnoreCaseAndTenantId(newName, tenantId)) {
                throw new ConflictException("Customer name already exists in this tenant");
            }

            customer.setName(newName);
        }

        if (request.email() != null) {
            customer.setEmail(normalizeNullable(request.email()));
        }
        if (request.phone() != null) {
            customer.setPhone(normalizeNullable(request.phone()));
        }
        if (request.address() != null) {
            customer.setAddress(normalizeNullable(request.address()));
        }
        if (request.taxId() != null) {
            customer.setTaxId(normalizeNullable(request.taxId()));
        }
        if (request.categoryId() != null) {
            customer.setCategory(resolveCategory(request.categoryId(), tenantId));
        }

        return toResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customerRepository.delete(customer);
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getTaxId(),
                customer.getCategory() != null ? customer.getCategory().getId() : null,
                customer.getCategory() != null ? customer.getCategory().getName() : null,
                customer.getTenantId(),
                customer.getCreatedAt()
        );
    }

    private CustomerCategory resolveCategory(Long categoryId, Long tenantId) {
        if (categoryId == null) {
            return null;
        }

        return customerCategoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer category not found"));
    }

    private CustomerCategory resolveCategoryOrDefault(Long categoryId, Long tenantId) {
        if (categoryId != null) {
            return resolveCategory(categoryId, tenantId);
        }

        return customerCategoryRepository.findFirstByTenantIdAndDefaultCategoryTrueOrderByIdAsc(tenantId)
                .orElseThrow(() -> new BadRequestException("No default customer category is configured"));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

