package com.gpms.backend.transport.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.transport.api.dto.TransportResponse;
import com.gpms.backend.transport.api.dto.TransportUpsertRequest;
import com.gpms.backend.transport.domain.Transport;
import com.gpms.backend.transport.infrastructure.TransportRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransportService {

    private final TransportRepository transportRepository;
    private final TransportMapper transportMapper;

    public TransportService(TransportRepository transportRepository, TransportMapper transportMapper) {
        this.transportRepository = transportRepository;
        this.transportMapper = transportMapper;
    }

    /** Everything, enabled or not - the management screen needs both. */
    @Transactional(readOnly = true)
    public List<TransportResponse> getAll() {
        return transportRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .map(transportMapper::toResponse)
                .toList();
    }

    /** Enabled only - what a dropdown is allowed to offer. */
    @Transactional(readOnly = true)
    public List<TransportResponse> getActive() {
        return transportRepository.findAllByDeletedFalseAndActiveTrueOrderByNameAsc()
                .stream()
                .map(transportMapper::toResponse)
                .toList();
    }

    public TransportResponse create(TransportUpsertRequest request) {
        transportRepository.findByCodeIgnoreCaseAndDeletedFalse(request.code())
                .ifPresent(existing -> {
                    throw new ConflictException("Transport code already exists");
                });
        transportRepository.findByNameIgnoreCaseAndDeletedFalse(request.name())
                .ifPresent(existing -> {
                    throw new ConflictException("Transport name already exists");
                });

        Transport transport = new Transport();
        apply(request, transport);
        return transportMapper.toResponse(transportRepository.save(transport));
    }

    public TransportResponse update(UUID id, TransportUpsertRequest request) {
        Transport transport = require(id);

        transportRepository.findByCodeIgnoreCaseAndDeletedFalse(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Transport code already exists");
                });
        transportRepository.findByNameIgnoreCaseAndDeletedFalse(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Transport name already exists");
                });

        apply(request, transport);
        return transportMapper.toResponse(transportRepository.save(transport));
    }

    /**
     * Enable or disable. Kept separate from update so the management
     * screen can flip status without re-submitting - and re-validating
     * - the whole record.
     */
    public TransportResponse setActive(UUID id, boolean active) {
        Transport transport = require(id);
        transport.setActive(active);
        return transportMapper.toResponse(transportRepository.save(transport));
    }

    /**
     * Soft delete. The row stays so that a gate pass raised last month
     * still resolves the transport it was raised against.
     */
    public void delete(UUID id) {
        Transport transport = require(id);
        transport.setDeleted(true);
        transport.setDeletedAt(Instant.now());
        transportRepository.save(transport);
    }

    private Transport require(UUID id) {
        return transportRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport not found"));
    }

    private void apply(TransportUpsertRequest request, Transport transport) {
        transport.setCode(request.code().trim().toUpperCase());
        transport.setName(request.name().trim());
        transport.setContactPerson(request.contactPerson());
        transport.setPhoneNumber(request.phoneNumber());
        transport.setEmail(request.email());
        transport.setActive(request.active() == null || request.active());
    }
}
