package com.gpms.backend.vehicle.api;

import com.gpms.backend.vehicle.api.dto.VehicleResponse;
import com.gpms.backend.vehicle.api.dto.VehicleUpsertRequest;
import com.gpms.backend.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleResponse> getAll() {
        return vehicleService.getAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public VehicleResponse create(@Valid @RequestBody VehicleUpsertRequest request) {
        return vehicleService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody VehicleUpsertRequest request) {
        return vehicleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(@PathVariable UUID id) {
        vehicleService.delete(id);
    }
}
