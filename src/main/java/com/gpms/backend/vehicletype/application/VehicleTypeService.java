package com.gpms.backend.vehicletype.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.vehicletype.api.dto.VehicleTypeResponse;
import com.gpms.backend.vehicletype.api.dto.VehicleTypeUpsertRequest;
import com.gpms.backend.vehicletype.domain.VehicleType;
import com.gpms.backend.vehicletype.infrastructure.VehicleTypeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeMapper vehicleTypeMapper;

    public VehicleTypeService(VehicleTypeRepository vehicleTypeRepository, VehicleTypeMapper vehicleTypeMapper) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleTypeMapper = vehicleTypeMapper;
    }

    /** Everything, enabled or not - the management screen needs both. */
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getAll() {
        return vehicleTypeRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .map(vehicleTypeMapper::toResponse)
                .toList();
    }

    /** Enabled only - what a dropdown is allowed to offer. */
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getActive() {
        return vehicleTypeRepository.findAllByDeletedFalseAndActiveTrueOrderByNameAsc()
                .stream()
                .map(vehicleTypeMapper::toResponse)
                .toList();
    }

    public VehicleTypeResponse create(VehicleTypeUpsertRequest request) {
        vehicleTypeRepository.findByNameIgnoreCaseAndDeletedFalse(request.name())
                .ifPresent(existing -> {
                    throw new ConflictException("Vehicle type name already exists");
                });

        VehicleType vehicleType = new VehicleType();
        apply(request, vehicleType);
        return vehicleTypeMapper.toResponse(vehicleTypeRepository.save(vehicleType));
    }

    public VehicleTypeResponse update(UUID id, VehicleTypeUpsertRequest request) {
        VehicleType vehicleType = require(id);

        vehicleTypeRepository.findByNameIgnoreCaseAndDeletedFalse(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Vehicle type name already exists");
                });

        apply(request, vehicleType);
        return vehicleTypeMapper.toResponse(vehicleTypeRepository.save(vehicleType));
    }

    /**
     * Enable or disable. Kept separate from update so the management
     * screen can flip status without re-submitting - and re-validating
     * - the whole record.
     */
    public VehicleTypeResponse setActive(UUID id, boolean active) {
        VehicleType vehicleType = require(id);
        vehicleType.setActive(active);
        return vehicleTypeMapper.toResponse(vehicleTypeRepository.save(vehicleType));
    }

    private VehicleType require(UUID id) {
        return vehicleTypeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found"));
    }

    private void apply(VehicleTypeUpsertRequest request, VehicleType vehicleType) {
        vehicleType.setName(request.name().trim());
        vehicleType.setActive(request.active() == null || request.active());
    }
}
