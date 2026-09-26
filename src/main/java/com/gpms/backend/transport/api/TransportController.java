package com.gpms.backend.transport.api;

import com.gpms.backend.transport.api.dto.TransportResponse;
import com.gpms.backend.transport.api.dto.TransportUpsertRequest;
import com.gpms.backend.transport.application.TransportService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/transports")
public class TransportController {

    private final TransportService transportService;

    public TransportController(TransportService transportService) {
        this.transportService = transportService;
    }

    /**
     * The management list. Any signed-in user may read it; only an
     * admin may change it.
     */
    @GetMapping
    public List<TransportResponse> getAll() {
        return transportService.getAll();
    }

    /** What the request form's dropdown loads. */
    @GetMapping("/active")
    public List<TransportResponse> getActive() {
        return transportService.getActive();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TransportResponse create(@Valid @RequestBody TransportUpsertRequest request) {
        return transportService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TransportResponse update(@PathVariable UUID id, @Valid @RequestBody TransportUpsertRequest request) {
        return transportService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public TransportResponse setActive(@PathVariable UUID id, @RequestParam boolean active) {
        return transportService.setActive(id, active);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        transportService.delete(id);
    }
}
