package com.gpms.backend.vehicletype.infrastructure;

import com.gpms.backend.vehicletype.domain.VehicleType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, UUID> {

    Optional<VehicleType> findByIdAndDeletedFalse(UUID id);

    Optional<VehicleType> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<VehicleType> findAllByDeletedFalseOrderByNameAsc();

    /**
     * Only these may appear in a dropdown. Disabling a record has to
     * take it out of circulation without deleting the history that
     * points at it.
     */
    List<VehicleType> findAllByDeletedFalseAndActiveTrueOrderByNameAsc();
}
