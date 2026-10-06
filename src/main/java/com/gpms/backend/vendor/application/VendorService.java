package com.gpms.backend.vendor.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.vendor.api.dto.VendorResponse;
import com.gpms.backend.vendor.api.dto.VendorUpsertRequest;
import com.gpms.backend.vendor.domain.Vendor;
import com.gpms.backend.vendor.infrastructure.VendorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;

@Service
@Transactional
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final CurrentUserService currentUserService;

    public VendorService(VendorRepository vendorRepository, VendorMapper vendorMapper,
            CurrentUserService currentUserService) {
        this.vendorRepository = vendorRepository;
        this.vendorMapper = vendorMapper;
        this.currentUserService = currentUserService;
    }

    public List<VendorResponse> getAll() {
        return vendorRepository.findAllByCompanyIdAndDeletedFalse(companyId()).stream().map(vendorMapper::toResponse).toList();
    }

    public VendorResponse create(VendorUpsertRequest request) {
        String code = resolveCode(request.code(), request.name(), null);

        vendorRepository.findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(request.name(), companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("A vendor with that name already exists");
                });
        vendorRepository.findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(code, companyId())
                .ifPresent(existing -> {
                    throw new ConflictException("Vendor code already exists");
                });
        Vendor vendor = new Vendor();
        vendor.setCompany(company());
        apply(request, vendor, code);
        return vendorMapper.toResponse(vendorRepository.save(vendor));
    }

    public VendorResponse update(UUID id, VendorUpsertRequest request) {
        Vendor vendor = vendorRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        String code = request.code() == null || request.code().isBlank()
                ? vendor.getCode()
                : request.code().trim().toUpperCase();

        vendorRepository.findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(code, companyId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Vendor code already exists");
                });
        apply(request, vendor, code);
        return vendorMapper.toResponse(vendorRepository.save(vendor));
    }

    public void delete(UUID id) {
        Vendor vendor = vendorRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        vendor.setDeleted(true);
        vendor.setDeletedAt(Instant.now());
        vendorRepository.save(vendor);
    }

    private void apply(VendorUpsertRequest request, Vendor vendor, String code) {
        vendor.setCode(code);
        vendor.setName(request.name().trim());
        vendor.setContactPerson(request.contactPerson());
        vendor.setPhoneNumber(request.phoneNumber());
        vendor.setEmail(request.email());
        vendor.setActive(request.active() == null || request.active());
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
     * The app's manage-vendors screen asks for a name and nothing
     * else, which is the right question to ask - a code is an internal
     * handle, not something an admin should have to invent. Requiring it
     * is what made "add a vendor" come back as a validation error.
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
            base = "VENDOR";
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
        return vendorRepository
                .findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(code, companyId())
                .filter(existing -> ignoreId == null || !existing.getId().equals(ignoreId))
                .isPresent();
    }
}
