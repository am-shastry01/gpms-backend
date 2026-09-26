package com.gpms.backend.signature.api;

import com.gpms.backend.signature.api.dto.DigitalSignatureResponse;
import com.gpms.backend.signature.application.DigitalSignatureService;
import com.gpms.backend.signature.domain.DigitalSignature;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Digital signature management. Admin only - this decides whose mark
 * ends up on a legal-ish document.
 */
@RestController
@RequestMapping("/api/v1/signatures")
@PreAuthorize("hasRole('ADMIN')")
public class DigitalSignatureController {

    private final DigitalSignatureService digitalSignatureService;

    public DigitalSignatureController(DigitalSignatureService digitalSignatureService) {
        this.digitalSignatureService = digitalSignatureService;
    }

    @GetMapping
    public List<DigitalSignatureResponse> listCurrent() {
        return digitalSignatureService.listCurrent();
    }

    @PostMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DigitalSignatureResponse upload(
            @PathVariable UUID userId,
            @RequestPart("file") MultipartFile file
    ) {
        return digitalSignatureService.upload(userId, file);
    }

    @GetMapping("/{signatureId}/image")
    public ResponseEntity<byte[]> image(@PathVariable UUID signatureId) {
        DigitalSignature signature = digitalSignatureService.requireImage(signatureId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + signature.getFileName())
                .contentType(MediaType.parseMediaType(signature.getContentType()))
                .body(signature.getImageData());
    }

    @DeleteMapping("/{userId}")
    public void remove(@PathVariable UUID userId) {
        digitalSignatureService.remove(userId);
    }
}
