package com.gpms.backend.gatepass.api;

import com.gpms.backend.common.api.PageResponse;
import com.gpms.backend.gatepass.api.dto.AttachmentResponse;
import com.gpms.backend.gatepass.api.dto.GateActionRequest;
import com.gpms.backend.gatepass.api.dto.GatePassCreateRequest;
import com.gpms.backend.gatepass.api.dto.GatePassResponse;
import com.gpms.backend.gatepass.application.GatePassRequestService;
import com.gpms.backend.gatepass.domain.GatePassStatus;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/gate-pass-requests")
public class GatePassRequestController {

    private final GatePassRequestService gatePassRequestService;

    public GatePassRequestController(GatePassRequestService gatePassRequestService) {
        this.gatePassRequestService = gatePassRequestService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public GatePassResponse create(@Valid @RequestBody GatePassCreateRequest request) {
        return gatePassRequestService.create(request);
    }

    /**
     * Returns a stable page shape rather than Spring's PageImpl,
     * whose JSON structure is not a supported contract.
     */
    @GetMapping
    public PageResponse<GatePassResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) GatePassStatus status,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(
                gatePassRequestService.list(query, status, warehouseId, fromDate, toDate, page, size)
        );
    }

    @GetMapping("/{id}")
    public GatePassResponse getById(@PathVariable UUID id) {
        return gatePassRequestService.getById(id);
    }

    /**
     * Withdraws a request that has not been decided on yet.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public GatePassResponse cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) GateActionRequest request
    ) {
        return gatePassRequestService.cancel(id, request);
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse addAttachment(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ) {
        return gatePassRequestService.addAttachment(id, file);
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getQrCode(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=gate-pass-qr.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(gatePassRequestService.getQrPng(id));
    }
}
