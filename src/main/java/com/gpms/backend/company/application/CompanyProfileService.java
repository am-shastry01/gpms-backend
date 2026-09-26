package com.gpms.backend.company.application;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.company.api.dto.CompanyProfileResponse;
import com.gpms.backend.company.api.dto.CompanyProfileUpdateRequest;
import com.gpms.backend.company.domain.CompanyProfile;
import com.gpms.backend.company.infrastructure.CompanyProfileRepository;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class CompanyProfileService {

    private static final long MAX_LOGO_BYTES = 2L * 1024L * 1024L;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg");

    private final CompanyProfileRepository companyProfileRepository;

    public CompanyProfileService(CompanyProfileRepository companyProfileRepository) {
        this.companyProfileRepository = companyProfileRepository;
    }

    /**
     * The one profile.
     *
     * Read-only on purpose. This is called while RENDERING a PDF,
     * which runs inside a read-only transaction - persisting a
     * fallback row from in here would join that transaction and fail
     * on "Connection is read-only". V9 seeds the row, so the absent
     * case is a misconfigured database rather than a normal state,
     * and an unsaved in-memory default is the honest answer: the
     * document still renders, and nothing silently writes.
     */
    @Transactional(readOnly = true)
    public CompanyProfile require() {
        return companyProfileRepository
                .findByCodeAndDeletedFalse(CompanyProfile.SINGLETON_CODE)
                .orElseGet(CompanyProfileService::unsavedDefault);
    }

    /**
     * Persisting variant, for the paths that legitimately write -
     * updating the profile or its logo.
     */
    private CompanyProfile requireForUpdate() {
        return companyProfileRepository
                .findByCodeAndDeletedFalse(CompanyProfile.SINGLETON_CODE)
                .orElseGet(() -> companyProfileRepository.save(unsavedDefault()));
    }

    private static CompanyProfile unsavedDefault() {
        CompanyProfile profile = new CompanyProfile();
        profile.setCode(CompanyProfile.SINGLETON_CODE);
        profile.setName("Your Company Name");
        return profile;
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse get() {
        return toResponse(require());
    }

    public CompanyProfileResponse update(CompanyProfileUpdateRequest request) {
        CompanyProfile profile = requireForUpdate();
        profile.setName(request.name().trim());
        profile.setAddressLine1(request.addressLine1());
        profile.setAddressLine2(request.addressLine2());
        profile.setCity(request.city());
        profile.setState(request.state());
        profile.setPostalCode(request.postalCode());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setEmail(request.email());
        profile.setGstNumber(request.gstNumber());
        return toResponse(companyProfileRepository.save(profile));
    }

    public CompanyProfileResponse uploadLogo(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Logo image is required");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessException("Logo must be 2 MB or smaller");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase();

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("Logo must be a PNG or JPEG image");
        }

        CompanyProfile profile = requireForUpdate();
        try {
            profile.setLogoData(file.getBytes());
        } catch (IOException exception) {
            throw new BusinessException("Could not read the uploaded logo");
        }
        profile.setLogoContentType(contentType);
        return toResponse(companyProfileRepository.save(profile));
    }

    private CompanyProfileResponse toResponse(CompanyProfile profile) {
        return new CompanyProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getAddressLine1(),
                profile.getAddressLine2(),
                profile.getCity(),
                profile.getState(),
                profile.getPostalCode(),
                profile.getPhoneNumber(),
                profile.getEmail(),
                profile.getGstNumber(),
                profile.getLogoData() != null && profile.getLogoData().length > 0
        );
    }
}
