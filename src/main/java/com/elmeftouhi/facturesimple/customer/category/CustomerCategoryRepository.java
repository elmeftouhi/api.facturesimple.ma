package com.elmeftouhi.facturesimple.customer.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerCategoryRepository extends JpaRepository<CustomerCategory, Long> {

    List<CustomerCategory> findAllByTenantIdOrderByIdDesc(Long tenantId);

    Optional<CustomerCategory> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByNameIgnoreCaseAndTenantId(String name, Long tenantId);

    boolean existsByTenantIdAndDefaultCategoryTrue(Long tenantId);

    Optional<CustomerCategory> findFirstByTenantIdAndDefaultCategoryTrueOrderByIdAsc(Long tenantId);

    @Modifying
    @Query("""
            update CustomerCategory cc
               set cc.defaultCategory = false
             where cc.tenantId = :tenantId
               and cc.defaultCategory = true
            """)
    int clearDefaultByTenantId(@Param("tenantId") Long tenantId);
}

