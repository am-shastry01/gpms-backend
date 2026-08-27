package com.gpms.backend.gatepass.api;

import com.gpms.backend.gatepass.api.dto.ExitConfirmationRequest;
import com.gpms.backend.gatepass.api.dto.GatePassResponse;
import com.gpms.backend.gatepass.application.GatePassRequestService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
@PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
public class SecurityController {

    private final GatePassRequestService gatePassRequestService;

    public SecurityController(GatePassRequestService gatePassRequestService) {
        this.gatePassRequestService = gatePassRequestService;
    }

    @PostMapping("/gate-pass-requests/{requestId}/exit")
    public GatePassResponse markExit(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ExitConfirmationRequest request
    ) {
        return gatePassRequestService.markExit(
                requestId,
                request == null ? new ExitConfirmationRequest(null) : request
        );
    }
}
