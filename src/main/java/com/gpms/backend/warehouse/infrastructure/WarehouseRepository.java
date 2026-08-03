package com.gpms.backend.warehouse.infrastructure;

import com.gpms.backend.warehouse.domain.Warehouse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    Optional<Warehouse> findByIdAndDeletedFalse(UUID id);

    Optional<Warehouse> findByCodeIgnoreCaseAndDeletedFalse(String code);

    List<Warehouse> findAllByDeletedFalse();
}
