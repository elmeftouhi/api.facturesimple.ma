package com.elmeftouhi.facturesimple.provider.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderCategoryRepository extends JpaRepository<ProviderCategory, Long> {

    List<ProviderCategory> findAllByTenantIdOrderByIdDesc(Long tenantId);

    Optional<ProviderCategory> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByNameIgnoreCaseAndTenantId(String name, Long tenantId);

    boolean existsByTenantIdAndDefaultCategoryTrue(Long tenantId);

    Optional<ProviderCategory> findFirstByTenantIdAndDefaultCategoryTrueOrderByIdAsc(Long tenantId);

    @Modifying
    @Query("""
            update ProviderCategory pc
               set pc.defaultCategory = false
              where pc.tenantId = :tenantId
                and pc.defaultCategory = true
             """)
    int clearDefaultByTenantId(@Param("tenantId") Long tenantId);
}

