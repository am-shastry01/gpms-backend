package com.gpms.backend.warehouse.api;

import com.gpms.backend.warehouse.api.dto.WarehouseResponse;
import com.gpms.backend.warehouse.api.dto.WarehouseUpsertRequest;
import com.gpms.backend.warehouse.application.WarehouseService;
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
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public List<WarehouseResponse> getAll() {
        return warehouseService.getAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse create(@Valid @RequestBody WarehouseUpsertRequest request) {
        return warehouseService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse update(@PathVariable UUID id, @Valid @RequestBody WarehouseUpsertRequest request) {
        return warehouseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        warehouseService.delete(id);
    }
}
