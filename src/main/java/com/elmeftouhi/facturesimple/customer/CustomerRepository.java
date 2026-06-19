package com.elmeftouhi.facturesimple.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByTenantIdOrderByIdDesc(Long tenantId);

    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByNameIgnoreCaseAndTenantId(String name, Long tenantId);

    boolean existsByCategory_IdAndTenantId(Long categoryId, Long tenantId);
}

