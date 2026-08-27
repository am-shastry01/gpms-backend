package com.gpms.backend.gatepass.api;

import com.gpms.backend.gatepass.api.dto.ApprovalActionRequest;
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
@RequestMapping("/api/v1/approvals")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ApprovalController {

    private final GatePassRequestService gatePassRequestService;

    public ApprovalController(GatePassRequestService gatePassRequestService) {
        this.gatePassRequestService = gatePassRequestService;
    }

    @PostMapping("/{requestId}/approve")
    public GatePassResponse approve(@PathVariable UUID requestId, @RequestBody ApprovalActionRequest request) {
        return gatePassRequestService.approve(requestId, request);
    }

    @PostMapping("/{requestId}/reject")
    public GatePassResponse reject(@PathVariable UUID requestId, @RequestBody ApprovalActionRequest request) {
        return gatePassRequestService.reject(requestId, request);
    }
}
