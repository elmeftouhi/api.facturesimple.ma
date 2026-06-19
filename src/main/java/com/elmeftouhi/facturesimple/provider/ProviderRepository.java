package com.elmeftouhi.facturesimple.provider;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    List<Provider> findAllByTenantIdOrderByIdDesc(Long tenantId);

    Optional<Provider> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByNameIgnoreCaseAndTenantId(String name, Long tenantId);

    boolean existsByCategory_IdAndTenantId(Long categoryId, Long tenantId);
}

