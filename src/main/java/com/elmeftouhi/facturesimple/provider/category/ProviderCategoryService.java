package com.elmeftouhi.facturesimple.provider.category;

import com.elmeftouhi.facturesimple.multitenancy.TenantContext;
import com.elmeftouhi.facturesimple.provider.ProviderRepository;
import com.elmeftouhi.facturesimple.provider.category.dto.ProviderCategoryCreateRequest;
import com.elmeftouhi.facturesimple.provider.category.dto.ProviderCategoryResponse;
import com.elmeftouhi.facturesimple.provider.category.dto.ProviderCategoryUpdateRequest;
import com.elmeftouhi.facturesimple.shared.exception.BadRequestException;
import com.elmeftouhi.facturesimple.shared.exception.ConflictException;
import com.elmeftouhi.facturesimple.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderCategoryService {

    private final ProviderCategoryRepository providerCategoryRepository;
    private final ProviderRepository providerRepository;

    @Transactional
    public ProviderCategoryResponse create(ProviderCategoryCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        String name = request.name().trim();
        if (providerCategoryRepository.existsByNameIgnoreCaseAndTenantId(name, tenantId)) {
            throw new ConflictException("Provider category name already exists in this tenant");
        }

        ProviderCategory category = new ProviderCategory();
        category.setName(name);
        category.setDescription(normalizeNullable(request.description()));
        category.setDefaultCategory(shouldSetAsDefaultOnCreate(request.isDefault(), tenantId));

        if (category.isDefaultCategory()) {
            providerCategoryRepository.clearDefaultByTenantId(tenantId);
        }

        ProviderCategory saved = providerCategoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderCategoryResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return providerCategoryRepository.findAllByTenantIdOrderByIdDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderCategoryResponse findById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        ProviderCategory category = providerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider category not found"));
        return toResponse(category);
    }

    @Transactional
    public ProviderCategoryResponse update(Long id, ProviderCategoryUpdateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        ProviderCategory category = providerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider category not found"));

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("Provider category name cannot be blank");
            }

            if (!category.getName().equalsIgnoreCase(name)
                    && providerCategoryRepository.existsByNameIgnoreCaseAndTenantId(name, tenantId)) {
                throw new ConflictException("Provider category name already exists in this tenant");
            }

            category.setName(name);
        }

        if (request.description() != null) {
            category.setDescription(normalizeNullable(request.description()));
        }

        return toResponse(category);
    }

    @Transactional
    public ProviderCategoryResponse setDefault(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        ProviderCategory category = providerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider category not found"));

        if (!category.isDefaultCategory()) {
            providerCategoryRepository.clearDefaultByTenantId(tenantId);
            category.setDefaultCategory(true);
        }

        return toResponse(category);
    }

    @Transactional
    public void unsetDefault(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        ProviderCategory category = providerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider category not found"));

        if (category.isDefaultCategory()) {
            category.setDefaultCategory(false);
        }
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        ProviderCategory category = providerCategoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider category not found"));

        if (category.isDefaultCategory()) {
            throw new BadRequestException("Default provider category cannot be deleted");
        }

        if (providerRepository.existsByCategory_IdAndTenantId(category.getId(), tenantId)) {
            throw new ConflictException("Provider category is used by existing providers");
        }

        providerCategoryRepository.delete(category);
    }

    private ProviderCategoryResponse toResponse(ProviderCategory category) {
        return new ProviderCategoryResponse(
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
        return !providerCategoryRepository.existsByTenantIdAndDefaultCategoryTrue(tenantId);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

