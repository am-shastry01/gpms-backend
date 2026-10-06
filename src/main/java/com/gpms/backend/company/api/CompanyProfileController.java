package com.gpms.backend.company.api;

import com.gpms.backend.company.api.dto.CompanyProfileResponse;
import com.gpms.backend.company.api.dto.CompanyProfileUpdateRequest;
import com.gpms.backend.company.application.CompanyProfileService;
import com.gpms.backend.company.domain.Company;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/company-profile")
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    /** Readable by anyone signed in - the app shows it on the pass. */
    @GetMapping
    public CompanyProfileResponse get() {
        return companyProfileService.get();
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        Company profile = companyProfileService.require();
        if (profile.getLogoData() == null || profile.getLogoData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=company-logo")
                .contentType(MediaType.parseMediaType(
                        profile.getLogoContentType() == null
                                ? MediaType.IMAGE_PNG_VALUE
                                : profile.getLogoContentType()))
                .body(profile.getLogoData());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyProfileResponse update(@Valid @RequestBody CompanyProfileUpdateRequest request) {
        return companyProfileService.update(request);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyProfileResponse uploadLogo(@RequestPart("file") MultipartFile file) {
        return companyProfileService.uploadLogo(file);
    }
}
