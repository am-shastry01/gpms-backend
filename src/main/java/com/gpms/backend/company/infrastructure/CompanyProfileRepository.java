package com.gpms.backend.company.infrastructure;

import com.gpms.backend.company.domain.CompanyProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {

    Optional<CompanyProfile> findByCodeAndDeletedFalse(String code);
}
