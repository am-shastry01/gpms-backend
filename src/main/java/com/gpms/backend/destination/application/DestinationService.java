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
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;

@Service
@Transactional
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationMapper destinationMapper;
    private final CurrentUserService currentUserService;

    public DestinationService(DestinationRepository destinationRepository, DestinationMapper destinationMapper,
            CurrentUserService currentUserService) {
        this.destinationRepository = destinationRepository;
        this.destinationMapper = destinationMapper;
        this.currentUserService = currentUserService;
    }

    /** Everything, enabled or not - the management screen needs both. */
    @Transactional(readOnly = true)
    public List<DestinationResponse> getAll() {
        return destinationRepository.findAllByCompanyIdAndDeletedFalseOrderByNameAsc(companyId())
                .stream()
                .map(destinationMapper::toResponse)
                .toList();
    }

    /** Enabled only - what a dropdown is allowed to offer. */
    @Transactional(readOnly = true)
    public List<DestinationResponse> getActive() {
        return destinationRepository.findAllByCompanyIdAndDeletedFalseAndActiveTrueOrderByNameAsc(companyId())
                .stream()
                .map(destinationMapper::toResponse)
                .toList();
    }

    public DestinationResponse create(DestinationUpsertRequest request) {
        destinationRepository.findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(request.name(), companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Destination name already exists");
                });

        Destination destination = new Destination();
        destination.setCompany(company());
        apply(request, destination);
        return destinationMapper.toResponse(destinationRepository.save(destination));
    }

    public DestinationResponse update(UUID id, DestinationUpsertRequest request) {
        Destination destination = require(id);

        destinationRepository.findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(request.name(), companyId())
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
        return destinationRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
    }

    private void apply(DestinationUpsertRequest request, Destination destination) {
        destination.setName(request.name().trim());
        destination.setActive(request.active() == null || request.active());
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
