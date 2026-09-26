package com.gpms.backend.vehicletype.api;

import com.gpms.backend.vehicletype.api.dto.VehicleTypeResponse;
import com.gpms.backend.vehicletype.api.dto.VehicleTypeUpsertRequest;
import com.gpms.backend.vehicletype.application.VehicleTypeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicle-types")
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    public VehicleTypeController(VehicleTypeService vehicleTypeService) {
        this.vehicleTypeService = vehicleTypeService;
    }

    /**
     * The management list. Any signed-in user may read it; only an
     * admin may change it.
     */
    @GetMapping
    public List<VehicleTypeResponse> getAll() {
        return vehicleTypeService.getAll();
    }

    /** What the request form's dropdown loads. */
    @GetMapping("/active")
    public List<VehicleTypeResponse> getActive() {
        return vehicleTypeService.getActive();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public VehicleTypeResponse create(@Valid @RequestBody VehicleTypeUpsertRequest request) {
        return vehicleTypeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VehicleTypeResponse update(@PathVariable UUID id, @Valid @RequestBody VehicleTypeUpsertRequest request) {
        return vehicleTypeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public VehicleTypeResponse setActive(@PathVariable UUID id, @RequestParam boolean active) {
        return vehicleTypeService.setActive(id, active);
    }
}
