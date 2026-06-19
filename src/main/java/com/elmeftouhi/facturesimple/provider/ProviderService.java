package com.elmeftouhi.facturesimple.provider;

import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.provider.category.ProviderCategory;
import com.elmeftouhi.facturesimple.provider.category.ProviderCategoryRepository;
import com.elmeftouhi.facturesimple.provider.dto.ProviderCreateRequest;
import com.elmeftouhi.facturesimple.provider.dto.ProviderResponse;
import com.elmeftouhi.facturesimple.provider.dto.ProviderUpdateRequest;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ProviderCategoryRepository providerCategoryRepository;

    @Transactional
    public ProviderResponse create(ProviderCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        if (providerRepository.existsByNameIgnoreCaseAndTenantId(request.name(), tenantId)) {
            throw new ConflictException("Provider name already exists in this tenant");
        }

        Provider provider = new Provider();
        provider.setName(request.name().trim());
        provider.setEmail(normalizeNullable(request.email()));
        provider.setPhone(normalizeNullable(request.phone()));
        provider.setAddress(normalizeNullable(request.address()));
        provider.setTaxId(normalizeNullable(request.taxId()));
        provider.setCategory(resolveCategoryOrDefault(request.categoryId(), tenantId));

        Provider saved = providerRepository.save(provider);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return providerRepository.findAllByTenantIdOrderByIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderResponse findById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Provider provider = providerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        return toResponse(provider);
    }

    @Transactional
    public ProviderResponse update(Long id, ProviderUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Provider provider = providerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw new BadRequestException("Provider name cannot be blank");
            }

            if (!provider.getName().equalsIgnoreCase(newName)
                    && providerRepository.existsByNameIgnoreCaseAndTenantId(newName, tenantId)) {
                throw new ConflictException("Provider name already exists in this tenant");
            }

            provider.setName(newName);
        }

        if (request.email() != null) {
            provider.setEmail(normalizeNullable(request.email()));
        }
        if (request.phone() != null) {
            provider.setPhone(normalizeNullable(request.phone()));
        }
        if (request.address() != null) {
            provider.setAddress(normalizeNullable(request.address()));
        }
        if (request.taxId() != null) {
            provider.setTaxId(normalizeNullable(request.taxId()));
        }
        if (request.categoryId() != null) {
            provider.setCategory(resolveCategory(request.categoryId(), tenantId));
        }

        return toResponse(provider);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Provider provider = providerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        providerRepository.delete(provider);
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getEmail(),
                provider.getPhone(),
                provider.getAddress(),
                provider.getTaxId(),
                provider.getCategory() != null ? provider.getCategory().getId() : null,
                provider.getCategory() != null ? provider.getCategory().getName() : null,
                provider.getTenantId(),
                provider.getCreatedAt()
        );
    }

    private ProviderCategory resolveCategory(Long categoryId, Long tenantId) {
        if (categoryId == null) {
            return null;
        }

        return providerCategoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider category not found"));
    }

    private ProviderCategory resolveCategoryOrDefault(Long categoryId, Long tenantId) {
        if (categoryId != null) {
            return resolveCategory(categoryId, tenantId);
        }

        return providerCategoryRepository.findFirstByTenantIdAndDefaultCategoryTrueOrderByIdAsc(tenantId)
                .orElseThrow(() -> new BadRequestException("No default provider category is configured"));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

