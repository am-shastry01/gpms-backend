package com.gpms.backend.vendor.api;

import com.gpms.backend.vendor.api.dto.VendorResponse;
import com.gpms.backend.vendor.api.dto.VendorUpsertRequest;
import com.gpms.backend.vendor.application.VendorService;
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
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public List<VendorResponse> getAll() {
        return vendorService.getAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public VendorResponse create(@Valid @RequestBody VendorUpsertRequest request) {
        return vendorService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public VendorResponse update(@PathVariable UUID id, @Valid @RequestBody VendorUpsertRequest request) {
        return vendorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(@PathVariable UUID id) {
        vendorService.delete(id);
    }
}
