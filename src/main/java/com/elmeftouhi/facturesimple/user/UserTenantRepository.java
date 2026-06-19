package com.elmeftouhi.facturesimple.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {

    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);

    List<UserTenant> findAllByUserId(Long userId);

    Optional<UserTenant> findByUserIdAndTenantId(Long userId, Long tenantId);


    long deleteByUserIdAndTenantId(Long userId, Long tenantId);
}

