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
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;

@Service
@Transactional
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeMapper vehicleTypeMapper;
    private final CurrentUserService currentUserService;

    public VehicleTypeService(VehicleTypeRepository vehicleTypeRepository, VehicleTypeMapper vehicleTypeMapper,
            CurrentUserService currentUserService) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleTypeMapper = vehicleTypeMapper;
        this.currentUserService = currentUserService;
    }

    /** Everything, enabled or not - the management screen needs both. */
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getAll() {
        return vehicleTypeRepository.findAllByCompanyIdAndDeletedFalseOrderByNameAsc(companyId())
                .stream()
                .map(vehicleTypeMapper::toResponse)
                .toList();
    }

    /** Enabled only - what a dropdown is allowed to offer. */
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getActive() {
        return vehicleTypeRepository.findAllByCompanyIdAndDeletedFalseAndActiveTrueOrderByNameAsc(companyId())
                .stream()
                .map(vehicleTypeMapper::toResponse)
                .toList();
    }

    public VehicleTypeResponse create(VehicleTypeUpsertRequest request) {
        vehicleTypeRepository.findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(request.name(), companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Vehicle type name already exists");
                });

        VehicleType vehicleType = new VehicleType();
        vehicleType.setCompany(company());
        apply(request, vehicleType);
        return vehicleTypeMapper.toResponse(vehicleTypeRepository.save(vehicleType));
    }

    public VehicleTypeResponse update(UUID id, VehicleTypeUpsertRequest request) {
        VehicleType vehicleType = require(id);

        vehicleTypeRepository.findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(request.name(), companyId())
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
        return vehicleTypeRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found"));
    }

    private void apply(VehicleTypeUpsertRequest request, VehicleType vehicleType) {
        vehicleType.setName(request.name().trim());
        vehicleType.setActive(request.active() == null || request.active());
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
