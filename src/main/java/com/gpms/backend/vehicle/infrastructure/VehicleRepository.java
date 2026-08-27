package com.gpms.backend.vehicle.infrastructure;

import com.gpms.backend.vehicle.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByIdAndDeletedFalse(UUID id);

    Optional<Vehicle> findByRegistrationNumberIgnoreCaseAndDeletedFalse(String registrationNumber);

    List<Vehicle> findAllByDeletedFalse();
}
