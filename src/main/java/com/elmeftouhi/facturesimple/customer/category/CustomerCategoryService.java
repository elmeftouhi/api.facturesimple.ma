package com.elmeftouhi.facturesimple.customer.category;

import com.elmeftouhi.facturesimple.customer.CustomerRepository;
import com.elmeftouhi.facturesimple.customer.category.dto.CustomerCategoryCreateRequest;
import com.elmeftouhi.facturesimple.customer.category.dto.CustomerCategoryResponse;
import com.elmeftouhi.facturesimple.customer.category.dto.CustomerCategoryUpdateRequest;
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
public class CustomerCategoryService {

    private final CustomerCategoryRepository customerCategoryRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerCategoryResponse create(CustomerCategoryCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        String name = request.name().trim();
        if (customerCategoryRepository.existsByNameIgnoreCaseAndTenantId(name, tenantId)) {
            throw new ConflictException("Customer category name already exists in this tenant");
        }

        CustomerCategory category = new CustomerCategory();
        category.setName(name);
        category.setDescription(normalizeNullable(request.description()));
        category.setDefaultCategory(shouldSetAsDefaultOnCreate(request.isDefault(), tenantId));

        if (category.isDefaultCategory()) {
            customerCategoryRepository.clearDefaultByTenantId(tenantId);
        }

        CustomerCategory saved = customerCategoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerCategoryResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return customerCategoryRepository.findAllByTenantIdOrderByIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerCategoryResponse findById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        CustomerCategory category = customerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer category not found"));
        return toResponse(category);
    }

    @Transactional
    public CustomerCategoryResponse update(Long id, CustomerCategoryUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        CustomerCategory category = customerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer category not found"));

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("Customer category name cannot be blank");
            }

            if (!category.getName().equalsIgnoreCase(name)
                    && customerCategoryRepository.existsByNameIgnoreCaseAndTenantId(name, tenantId)) {
                throw new ConflictException("Customer category name already exists in this tenant");
            }

            category.setName(name);
        }

        if (request.description() != null) {
            category.setDescription(normalizeNullable(request.description()));
        }

        return toResponse(category);
    }

    @Transactional
    public CustomerCategoryResponse setDefault(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        CustomerCategory category = customerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer category not found"));

        if (!category.isDefaultCategory()) {
            customerCategoryRepository.clearDefaultByTenantId(tenantId);
            category.setDefaultCategory(true);
        }

        return toResponse(category);
    }

    @Transactional
    public void unsetDefault(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        CustomerCategory category = customerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer category not found"));

        if (category.isDefaultCategory()) {
            category.setDefaultCategory(false);
        }
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        CustomerCategory category = customerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer category not found"));

        if (category.isDefaultCategory()) {
            throw new BadRequestException("Default customer category cannot be deleted");
        }

        if (customerRepository.existsByCategory_IdAndTenantId(category.getId(), tenantId)) {
            throw new ConflictException("Customer category is used by existing customers");
        }

        customerCategoryRepository.delete(category);
    }

    private CustomerCategoryResponse toResponse(CustomerCategory category) {
        return new CustomerCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isDefaultCategory(),
                category.getTenantId(),
                category.getCreatedAt()
        );
    }

    private boolean shouldSetAsDefaultOnCreate(Boolean requestedDefault, Long tenantId) {
        if (Boolean.TRUE.equals(requestedDefault)) {
            return true;
        }
        return !customerCategoryRepository.existsByTenantIdAndDefaultCategoryTrue(tenantId);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

