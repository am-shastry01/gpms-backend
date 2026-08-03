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

@Service
@Transactional
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    public VendorService(VendorRepository vendorRepository, VendorMapper vendorMapper) {
        this.vendorRepository = vendorRepository;
        this.vendorMapper = vendorMapper;
    }

    public List<VendorResponse> getAll() {
        return vendorRepository.findAllByDeletedFalse().stream().map(vendorMapper::toResponse).toList();
    }

    public VendorResponse create(VendorUpsertRequest request) {
        vendorRepository.findByCodeIgnoreCaseAndDeletedFalse(request.code())
                .ifPresent(existing -> {
                    throw new ConflictException("Vendor code already exists");
                });
        Vendor vendor = new Vendor();
        apply(request, vendor);
        return vendorMapper.toResponse(vendorRepository.save(vendor));
    }

    public VendorResponse update(UUID id, VendorUpsertRequest request) {
        Vendor vendor = vendorRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        vendorRepository.findByCodeIgnoreCaseAndDeletedFalse(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Vendor code already exists");
                });
        apply(request, vendor);
        return vendorMapper.toResponse(vendorRepository.save(vendor));
    }

    public void delete(UUID id) {
        Vendor vendor = vendorRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        vendor.setDeleted(true);
        vendor.setDeletedAt(Instant.now());
        vendorRepository.save(vendor);
    }

    private void apply(VendorUpsertRequest request, Vendor vendor) {
        vendor.setCode(request.code().trim().toUpperCase());
        vendor.setName(request.name().trim());
        vendor.setContactPerson(request.contactPerson());
        vendor.setPhoneNumber(request.phoneNumber());
        vendor.setEmail(request.email());
        vendor.setActive(request.active() == null || request.active());
    }
}
