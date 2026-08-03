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
}
