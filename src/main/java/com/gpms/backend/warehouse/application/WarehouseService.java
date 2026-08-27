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

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseRepository warehouseRepository, WarehouseMapper warehouseMapper) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseMapper = warehouseMapper;
    }

    public List<WarehouseResponse> getAll() {
        return warehouseRepository.findAllByDeletedFalse().stream().map(warehouseMapper::toResponse).toList();
    }

    public WarehouseResponse create(WarehouseUpsertRequest request) {
        warehouseRepository.findByCodeIgnoreCaseAndDeletedFalse(request.code())
                .ifPresent(existing -> {
                    throw new ConflictException("Warehouse code already exists");
                });
        Warehouse warehouse = new Warehouse();
        apply(request, warehouse);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public WarehouseResponse update(UUID id, WarehouseUpsertRequest request) {
        Warehouse warehouse = warehouseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouseRepository.findByCodeIgnoreCaseAndDeletedFalse(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Warehouse code already exists");
                });
        apply(request, warehouse);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public void delete(UUID id) {
        Warehouse warehouse = warehouseRepository.findByIdAndDeletedFalse(id)
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
}
