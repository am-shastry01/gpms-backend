package com.gpms.backend.driver.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.driver.api.dto.DriverResponse;
import com.gpms.backend.driver.api.dto.DriverUpsertRequest;
import com.gpms.backend.driver.domain.Driver;
import com.gpms.backend.driver.infrastructure.DriverRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public DriverService(DriverRepository driverRepository, DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
    }

    public List<DriverResponse> getAll() {
        return driverRepository.findAllByDeletedFalse().stream().map(driverMapper::toResponse).toList();
    }

    public DriverResponse create(DriverUpsertRequest request) {
        driverRepository.findByMobileNumberAndDeletedFalse(request.mobileNumber())
                .ifPresent(existing -> {
                    throw new ConflictException("Driver mobile number already exists");
                });
        Driver driver = new Driver();
        apply(request, driver);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    public DriverResponse update(UUID id, DriverUpsertRequest request) {
        Driver driver = driverRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        driverRepository.findByMobileNumberAndDeletedFalse(request.mobileNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Driver mobile number already exists");
                });
        apply(request, driver);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    public void delete(UUID id) {
        Driver driver = driverRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        driver.setDeleted(true);
        driver.setDeletedAt(Instant.now());
        driverRepository.save(driver);
    }

    private void apply(DriverUpsertRequest request, Driver driver) {
        driver.setName(request.name().trim());
        driver.setMobileNumber(request.mobileNumber().trim());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setActive(request.active() == null || request.active());
    }
}
