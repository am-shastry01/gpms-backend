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
}
