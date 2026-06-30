package com.elmeftouhi.facturesimple.company;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyBankRepository extends JpaRepository<CompanyBank, Long> {
    List<CompanyBank> findByCompanyId(Long companyId);

    Optional<CompanyBank> findByCompanyIdAndIsDefaultTrue(Long companyId);
}

