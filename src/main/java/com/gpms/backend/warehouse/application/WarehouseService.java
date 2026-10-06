package com.gpms.backend.warehouse.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.warehouse.api.dto.WarehouseResponse;
import com.gpms.backend.warehouse.api.dto.WarehouseUpsertRequest;
import com.gpms.backend.warehouse.domain.Warehouse;
import com.gpms.backend.warehouse.infrastructure.WarehouseRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final CurrentUserService currentUserService;

    public WarehouseService(
            WarehouseRepository warehouseRepository,
            WarehouseMapper warehouseMapper,
            CurrentUserService currentUserService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseMapper = warehouseMapper;
        this.currentUserService = currentUserService;
    }

    public List<WarehouseResponse> getAll() {
        return warehouseRepository
                .findAllByCompanyIdAndDeletedFalseOrderByNameAsc(companyId())
                .stream()
                .map(warehouseMapper::toResponse)
                .toList();
    }

    public WarehouseResponse create(WarehouseUpsertRequest request) {
        warehouseRepository.findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(
                        request.code(), companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Warehouse code already exists");
                });
        Warehouse warehouse = new Warehouse();
        warehouse.setCompany(company());
        apply(request, warehouse);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public WarehouseResponse update(UUID id, WarehouseUpsertRequest request) {
        Warehouse warehouse = warehouseRepository
                .findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouseRepository.findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(
                        request.code(), companyId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Warehouse code already exists");
                });
        apply(request, warehouse);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public void delete(UUID id) {
        Warehouse warehouse = warehouseRepository
                .findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouse.setDeleted(true);
        warehouse.setDeletedAt(Instant.now());
        warehouseRepository.save(warehouse);
    }

    private void apply(WarehouseUpsertRequest request, Warehouse warehouse) {
        warehouse.setCode(request.code().trim().toUpperCase());
        warehouse.setName(request.name().trim());
        warehouse.setLocation(request.location());
        warehouse.setAddressLine(request.addressLine());
        warehouse.setCity(request.city());
        warehouse.setState(request.state());
        warehouse.setCountry(request.country());
        warehouse.setTimezone(request.timezone());
        warehouse.setActive(request.active() == null || request.active());
    }

    /*
     * The caller's company. A warehouse list is one of the first things
     * the app asks for, and without this fence it answered with every
     * client's sites.
     */
    private Company company() {
        return currentUserService.requireCurrentUser().getCompany();
    }

    private UUID companyId() {
        return company().getId();
    }
}
