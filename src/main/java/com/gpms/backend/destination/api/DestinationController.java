package com.gpms.backend.destination.api;

import com.gpms.backend.destination.api.dto.DestinationResponse;
import com.gpms.backend.destination.api.dto.DestinationUpsertRequest;
import com.gpms.backend.destination.application.DestinationService;
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
@RequestMapping("/api/v1/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    /**
     * The management list. Any signed-in user may read it; only an
     * admin may change it.
     */
    @GetMapping
    public List<DestinationResponse> getAll() {
        return destinationService.getAll();
    }

    /** What the request form's dropdown loads. */
    @GetMapping("/active")
    public List<DestinationResponse> getActive() {
        return destinationService.getActive();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DestinationResponse create(@Valid @RequestBody DestinationUpsertRequest request) {
        return destinationService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DestinationResponse update(@PathVariable UUID id, @Valid @RequestBody DestinationUpsertRequest request) {
        return destinationService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public DestinationResponse setActive(@PathVariable UUID id, @RequestParam boolean active) {
        return destinationService.setActive(id, active);
    }
}
