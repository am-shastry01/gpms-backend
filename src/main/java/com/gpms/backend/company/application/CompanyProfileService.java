package com.gpms.backend.company.application;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.api.dto.CompanyProfileResponse;
import com.gpms.backend.company.api.dto.CompanyProfileUpdateRequest;
import com.gpms.backend.company.domain.Company;
import com.gpms.backend.company.infrastructure.CompanyRepository;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A company's own details, as that company's admin sees them.
 *
 * This used to be a singleton: one company_profile row, code DEFAULT,
 * shared by the whole deployment. With several clients on one database
 * there is no such thing as "the" company - so every call here resolves
 * to the caller's own, and an admin editing their letterhead can only
 * ever edit their own.
 *
 * The super-admin site writes these rows too, through its own service.
 * Both are editing the same table; the difference is that this one is
 * fenced to one row and that one is not.
 */
@Service
@Transactional
public class CompanyProfileService {

    private static final long MAX_LOGO_BYTES = 2L * 1024L * 1024L;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg");

    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUserService;

    public CompanyProfileService(
            CompanyRepository companyRepository,
            CurrentUserService currentUserService
    ) {
        this.companyRepository = companyRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * The caller's company.
     *
     * Read-only on purpose - it is called while rendering a PDF, inside
     * a read-only transaction, where persisting anything would fail on
     * "Connection is read-only".
     */
    @Transactional(readOnly = true)
    public Company require() {
        Company company = currentUserService.requireCurrentUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("Your account has no company");
        }

        /*
         * Re-read so the logo bytes and every other lazy field are
         * loaded off a managed entity rather than a proxy belonging to
         * someone else's transaction.
         */
        return companyRepository.findByIdAndDeletedFalse(company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse get() {
        return toResponse(require());
    }

    public CompanyProfileResponse update(CompanyProfileUpdateRequest request) {
        Company company = require();
        company.setName(request.name().trim());
        company.setAddressLine1(request.addressLine1());
        company.setAddressLine2(request.addressLine2());
        company.setCity(request.city());
        company.setState(request.state());
        company.setPostalCode(request.postalCode());
        company.setPhoneNumber(request.phoneNumber());
        company.setEmail(request.email());
        company.setGstNumber(request.gstNumber());
        company.setWebsite(request.website());
        return toResponse(companyRepository.save(company));
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

        Company company = require();

        try {
            company.setLogoData(file.getBytes());
        } catch (IOException exception) {
            throw new BusinessException("Could not read the logo image");
        }

        company.setLogoContentType(contentType);

        return toResponse(companyRepository.save(company));
    }

    private CompanyProfileResponse toResponse(Company company) {
        return new CompanyProfileResponse(
                company.getId(),
                company.getName(),
                company.getAddressLine1(),
                company.getAddressLine2(),
                company.getCity(),
                company.getState(),
                company.getPostalCode(),
                company.getPhoneNumber(),
                company.getEmail(),
                company.getGstNumber(),
                company.getWebsite(),
                company.getLogoData() != null && company.getLogoData().length > 0
        );
    }
}
