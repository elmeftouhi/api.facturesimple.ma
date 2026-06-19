package com.elmeftouhi.facturesimple.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantInviteRepository extends JpaRepository<TenantInvite, Long> {

    Optional<TenantInvite> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);
}

