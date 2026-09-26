package com.gpms.backend.destination.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.destination.api.dto.DestinationResponse;
import com.gpms.backend.destination.api.dto.DestinationUpsertRequest;
import com.gpms.backend.destination.domain.Destination;
import com.gpms.backend.destination.infrastructure.DestinationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationMapper destinationMapper;

    public DestinationService(DestinationRepository destinationRepository, DestinationMapper destinationMapper) {
        this.destinationRepository = destinationRepository;
        this.destinationMapper = destinationMapper;
    }

    /** Everything, enabled or not - the management screen needs both. */
    @Transactional(readOnly = true)
    public List<DestinationResponse> getAll() {
        return destinationRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .map(destinationMapper::toResponse)
                .toList();
    }

    /** Enabled only - what a dropdown is allowed to offer. */
    @Transactional(readOnly = true)
    public List<DestinationResponse> getActive() {
        return destinationRepository.findAllByDeletedFalseAndActiveTrueOrderByNameAsc()
                .stream()
                .map(destinationMapper::toResponse)
                .toList();
    }

    public DestinationResponse create(DestinationUpsertRequest request) {
        destinationRepository.findByNameIgnoreCaseAndDeletedFalse(request.name())
                .ifPresent(existing -> {
                    throw new ConflictException("Destination name already exists");
                });

        Destination destination = new Destination();
        apply(request, destination);
        return destinationMapper.toResponse(destinationRepository.save(destination));
    }

    public DestinationResponse update(UUID id, DestinationUpsertRequest request) {
        Destination destination = require(id);

        destinationRepository.findByNameIgnoreCaseAndDeletedFalse(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Destination name already exists");
                });

        apply(request, destination);
        return destinationMapper.toResponse(destinationRepository.save(destination));
    }

    /**
     * Enable or disable. Kept separate from update so the management
     * screen can flip status without re-submitting - and re-validating
     * - the whole record.
     */
    public DestinationResponse setActive(UUID id, boolean active) {
        Destination destination = require(id);
        destination.setActive(active);
        return destinationMapper.toResponse(destinationRepository.save(destination));
    }

    private Destination require(UUID id) {
        return destinationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
    }

    private void apply(DestinationUpsertRequest request, Destination destination) {
        destination.setName(request.name().trim());
        destination.setActive(request.active() == null || request.active());
    }
}
