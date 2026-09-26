package com.gpms.backend.gatepass.api;

import com.gpms.backend.gatepass.api.dto.ApprovalActionRequest;
import com.gpms.backend.gatepass.api.dto.BulkApprovalRequest;
import com.gpms.backend.gatepass.api.dto.BulkApprovalResponse;
import com.gpms.backend.gatepass.api.dto.GatePassResponse;
import com.gpms.backend.gatepass.application.BulkApprovalService;
import com.gpms.backend.gatepass.application.GatePassRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
/*
 * Employees sign too.
 *
 * A gate pass carries three blocks and any of the three roles can
 * raise one, so a manager-raised pass is waiting on an employee's
 * signature exactly as an employee-raised one waits on a manager's.
 * Rejection stays with managers and admins - see the method.
 */
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
public class ApprovalController {

    private final GatePassRequestService gatePassRequestService;
    private final BulkApprovalService bulkApprovalService;

    public ApprovalController(
            GatePassRequestService gatePassRequestService,
            BulkApprovalService bulkApprovalService
    ) {
        this.gatePassRequestService = gatePassRequestService;
        this.bulkApprovalService = bulkApprovalService;
    }

    /**
     * What this approver still has to sign, whoever else has signed
     * it already.
     */
    @GetMapping("/pending")
    public List<GatePassResponse> pending() {
        return gatePassRequestService.awaitingMySignature();
    }

    @PostMapping("/{requestId}/approve")
    public GatePassResponse approve(@PathVariable UUID requestId, @RequestBody ApprovalActionRequest request) {
        return gatePassRequestService.approve(requestId, request);
    }

    /**
     * Signs several passes at once - the tick-all action on the
     * pending screen.
     *
     * Reports how many went through and names the ones that did
     * not, rather than failing the whole batch on the first
     * problem.
     */
    @PostMapping("/bulk-approve")
    public BulkApprovalResponse bulkApprove(@Valid @RequestBody BulkApprovalRequest request) {
        return bulkApprovalService.approveAll(request);
    }

    /**
     * Turning a pass down is not the same as declining to sign it,
     * and it stays with the roles that can carry that decision.
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{requestId}/reject")
    public GatePassResponse reject(@PathVariable UUID requestId, @RequestBody ApprovalActionRequest request) {
        return gatePassRequestService.reject(requestId, request);
    }
}
