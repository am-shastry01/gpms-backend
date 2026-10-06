package com.gpms.backend.driver.infrastructure;

import com.gpms.backend.driver.domain.Driver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByIdAndDeletedFalse(UUID id);

    Optional<Driver> findByMobileNumberAndDeletedFalse(String mobileNumber);

    List<Driver> findAllByDeletedFalse();

    // ----- company-scoped (what the tenant API must use) -----

    Optional<Driver> findByIdAndCompanyIdAndDeletedFalse(UUID id, UUID companyId);

    Optional<Driver> findByMobileNumberAndCompanyIdAndDeletedFalse(String mobileNumber, UUID companyId);

    List<Driver> findAllByCompanyIdAndDeletedFalse(UUID companyId);
}
