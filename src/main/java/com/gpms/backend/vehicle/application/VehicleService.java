package com.gpms.backend.vehicle.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.vehicle.api.dto.VehicleResponse;
import com.gpms.backend.vehicle.api.dto.VehicleUpsertRequest;
import com.gpms.backend.vehicle.domain.Vehicle;
import com.gpms.backend.vehicle.infrastructure.VehicleRepository;
import com.gpms.backend.vendor.domain.Vendor;
import com.gpms.backend.vendor.infrastructure.VendorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VendorRepository vendorRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VendorRepository vendorRepository,
            VehicleMapper vehicleMapper
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vendorRepository = vendorRepository;
        this.vehicleMapper = vehicleMapper;
    }

    public List<VehicleResponse> getAll() {
        return vehicleRepository.findAllByDeletedFalse().stream().map(vehicleMapper::toResponse).toList();
    }

    public VehicleResponse create(VehicleUpsertRequest request) {
        vehicleRepository.findByRegistrationNumberIgnoreCaseAndDeletedFalse(request.registrationNumber())
                .ifPresent(existing -> {
                    throw new ConflictException("Vehicle registration number already exists");
                });
        Vehicle vehicle = new Vehicle();
        apply(request, vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    public VehicleResponse update(UUID id, VehicleUpsertRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicleRepository.findByRegistrationNumberIgnoreCaseAndDeletedFalse(request.registrationNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Vehicle registration number already exists");
                });
        apply(request, vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    public void delete(UUID id) {
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicle.setDeleted(true);
        vehicle.setDeletedAt(Instant.now());
        vehicleRepository.save(vehicle);
    }

    private void apply(VehicleUpsertRequest request, Vehicle vehicle) {
        Vendor vendor = null;
        if (request.vendorId() != null) {
            vendor = vendorRepository.findByIdAndDeletedFalse(request.vendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        }
        vehicle.setRegistrationNumber(request.registrationNumber().trim().toUpperCase());
        vehicle.setVehicleType(request.vehicleType().trim());
        vehicle.setCapacity(request.capacity());
        vehicle.setVendor(vendor);
        vehicle.setActive(request.active() == null || request.active());
    }
}
