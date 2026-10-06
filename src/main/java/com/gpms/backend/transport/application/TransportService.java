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
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;

@Service
@Transactional
public class TransportService {

    private final TransportRepository transportRepository;
    private final TransportMapper transportMapper;
    private final CurrentUserService currentUserService;

    public TransportService(TransportRepository transportRepository, TransportMapper transportMapper,
            CurrentUserService currentUserService) {
        this.transportRepository = transportRepository;
        this.transportMapper = transportMapper;
        this.currentUserService = currentUserService;
    }

    /** Everything, enabled or not - the management screen needs both. */
    @Transactional(readOnly = true)
    public List<TransportResponse> getAll() {
        return transportRepository.findAllByCompanyIdAndDeletedFalseOrderByNameAsc(companyId())
                .stream()
                .map(transportMapper::toResponse)
                .toList();
    }

    /** Enabled only - what a dropdown is allowed to offer. */
    @Transactional(readOnly = true)
    public List<TransportResponse> getActive() {
        return transportRepository.findAllByCompanyIdAndDeletedFalseAndActiveTrueOrderByNameAsc(companyId())
                .stream()
                .map(transportMapper::toResponse)
                .toList();
    }

    public TransportResponse create(TransportUpsertRequest request) {
        String code = resolveCode(request.code(), request.name(), null);

        transportRepository.findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(code, companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Transport code already exists");
                });
        transportRepository.findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(request.name(), companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Transport name already exists");
                });

        Transport transport = new Transport();
        transport.setCompany(company());
        apply(request, transport, code);
        return transportMapper.toResponse(transportRepository.save(transport));
    }

    public TransportResponse update(UUID id, TransportUpsertRequest request) {
        Transport transport = require(id);

        /*
         * A rename keeps the code it already has unless one was sent, so
         * renaming a transport company does not quietly renumber it.
         */
        String code = request.code() == null || request.code().isBlank()
                ? transport.getCode()
                : request.code().trim().toUpperCase();

        transportRepository.findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(code, companyId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Transport code already exists");
                });
        transportRepository.findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(request.name(), companyId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Transport name already exists");
                });

        apply(request, transport, code);
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
        return transportRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Transport not found"));
    }

    private void apply(TransportUpsertRequest request, Transport transport, String code) {
        transport.setCode(code);
        transport.setName(request.name().trim());
        transport.setContactPerson(request.contactPerson());
        transport.setPhoneNumber(request.phoneNumber());
        transport.setEmail(request.email());
        transport.setActive(request.active() == null || request.active());
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

    /*
     * The code, derived from the name when the caller did not send one.
     *
     * The app's manage-transport companies screen asks for a name and nothing
     * else, which is the right question to ask - a code is an internal
     * handle, not something an admin should have to invent. Requiring it
     * is what made "add Ekart" come back as a validation error.
     *
     * Letters and digits from the name, uppercased, at most 20
     * characters, with a number appended if that is already taken inside
     * this company.
     */
    private String resolveCode(String requested, String name, UUID ignoreId) {

        if (requested != null && !requested.isBlank()) {
            return requested.trim().toUpperCase();
        }

        String base = name == null ? "" : name.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

        if (base.isEmpty()) {
            base = "TRANSPORT";
        }

        base = base.substring(0, Math.min(base.length(), 20));

        String candidate = base;
        int suffix = 1;

        while (isCodeTaken(candidate, ignoreId)) {
            suffix++;
            candidate = base.substring(0, Math.min(base.length(), 18)) + suffix;
        }

        return candidate;
    }

    private boolean isCodeTaken(String code, UUID ignoreId) {
        return transportRepository
                .findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(code, companyId())
                .filter(existing -> ignoreId == null || !existing.getId().equals(ignoreId))
                .isPresent();
    }
}
