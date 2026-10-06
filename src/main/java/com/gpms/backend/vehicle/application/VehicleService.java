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
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VendorRepository vendorRepository;
    private final VehicleMapper vehicleMapper;
    private final CurrentUserService currentUserService;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VendorRepository vendorRepository,
            VehicleMapper vehicleMapper,
            CurrentUserService currentUserService) {
        this.vehicleRepository = vehicleRepository;
        this.vendorRepository = vendorRepository;
        this.vehicleMapper = vehicleMapper;
        this.currentUserService = currentUserService;
    }

    public List<VehicleResponse> getAll() {
        return vehicleRepository.findAllByCompanyIdAndDeletedFalse(companyId()).stream().map(vehicleMapper::toResponse).toList();
    }

    public VehicleResponse create(VehicleUpsertRequest request) {
        vehicleRepository.findByRegistrationNumberIgnoreCaseAndCompanyIdAndDeletedFalse(request.registrationNumber(), companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Vehicle registration number already exists");
                });
        Vehicle vehicle = new Vehicle();
        vehicle.setCompany(company());
        apply(request, vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    public VehicleResponse update(UUID id, VehicleUpsertRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicleRepository.findByRegistrationNumberIgnoreCaseAndCompanyIdAndDeletedFalse(request.registrationNumber(), companyId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Vehicle registration number already exists");
                });
        apply(request, vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    public void delete(UUID id) {
        Vehicle vehicle = vehicleRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicle.setDeleted(true);
        vehicle.setDeletedAt(Instant.now());
        vehicleRepository.save(vehicle);
    }

    private void apply(VehicleUpsertRequest request, Vehicle vehicle) {
        Vendor vendor = null;
        if (request.vendorId() != null) {
            /*
             * Scoped as well: an unscoped lookup here would let one
             * client attach their lorry to another client's vendor.
             */
            vendor = vendorRepository
                    .findByIdAndCompanyIdAndDeletedFalse(request.vendorId(), companyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        }
        vehicle.setRegistrationNumber(request.registrationNumber().trim().toUpperCase());
        vehicle.setVehicleType(request.vehicleType().trim());
        vehicle.setCapacity(request.capacity());
        vehicle.setVendor(vendor);
        vehicle.setActive(request.active() == null || request.active());
    }


    /*
     * The company the caller belongs to.
     *
     * Every read and every uniqueness check below is fenced by it, so
     * one client's records are invisible to another even though they
     * share a table. A lookup by id is scoped too - an id guessed or
     * copied from somewhere else resolves to nothing rather than to
     * somebody else's row.
     */
    private Company company() {
        return currentUserService.requireCurrentUser().getCompany();
    }

    private UUID companyId() {
        return company().getId();
    }
}
