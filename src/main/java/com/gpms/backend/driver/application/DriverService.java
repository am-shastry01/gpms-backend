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
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;
    private final CurrentUserService currentUserService;

    public DriverService(DriverRepository driverRepository, DriverMapper driverMapper,
            CurrentUserService currentUserService) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
        this.currentUserService = currentUserService;
    }

    public List<DriverResponse> getAll() {
        return driverRepository.findAllByCompanyIdAndDeletedFalse(companyId()).stream().map(driverMapper::toResponse).toList();
    }

    public DriverResponse create(DriverUpsertRequest request) {
        driverRepository.findByMobileNumberAndCompanyIdAndDeletedFalse(request.mobileNumber(), companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Driver mobile number already exists");
                });
        Driver driver = new Driver();
        driver.setCompany(company());
        apply(request, driver);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    public DriverResponse update(UUID id, DriverUpsertRequest request) {
        Driver driver = driverRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        driverRepository.findByMobileNumberAndCompanyIdAndDeletedFalse(request.mobileNumber(), companyId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Driver mobile number already exists");
                });
        apply(request, driver);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    public void delete(UUID id) {
        Driver driver = driverRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
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
