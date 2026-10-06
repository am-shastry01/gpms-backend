package com.gpms.backend.vendor.infrastructure;

import com.gpms.backend.vendor.domain.Vendor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByIdAndDeletedFalse(UUID id);

    Optional<Vendor> findByCodeIgnoreCaseAndDeletedFalse(String code);

    Optional<Vendor> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<Vendor> findAllByDeletedFalse();

    // ----- company-scoped (what the tenant API must use) -----

    Optional<Vendor> findByIdAndCompanyIdAndDeletedFalse(UUID id, UUID companyId);

    Optional<Vendor> findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(String code, UUID companyId);

    Optional<Vendor> findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(String name, UUID companyId);

    List<Vendor> findAllByCompanyIdAndDeletedFalse(UUID companyId);
}
