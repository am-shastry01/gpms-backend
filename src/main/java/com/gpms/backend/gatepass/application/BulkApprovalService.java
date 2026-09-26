package com.gpms.backend.gatepass.application;

import com.gpms.backend.gatepass.api.dto.ApprovalActionRequest;
import com.gpms.backend.gatepass.api.dto.BulkApprovalFailure;
import com.gpms.backend.gatepass.api.dto.BulkApprovalRequest;
import com.gpms.backend.gatepass.api.dto.BulkApprovalResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Signs several gate passes in one action.
 *
 * Deliberately NOT annotated @Transactional.
 *
 * Each pass has to succeed or fail on its own. If this method owned
 * a transaction, the first pass that threw - one already signed by
 * this role, say - would mark that transaction rollback-only and
 * silently undo every signature applied before it, while still
 * reporting them as successful. Leaving this method
 * non-transactional means each call into
 * GatePassRequestService.approve() opens its own transaction and
 * commits independently, which is the behaviour a partial batch
 * needs.
 *
 * It is a separate bean for the same reason: calling approve()
 * through `this` would bypass the proxy and lose the per-item
 * transaction entirely.
 */
@Service
public class BulkApprovalService {

    private final GatePassRequestService gatePassRequestService;

    public BulkApprovalService(GatePassRequestService gatePassRequestService) {
        this.gatePassRequestService = gatePassRequestService;
    }

    public BulkApprovalResponse approveAll(BulkApprovalRequest request) {

        ApprovalActionRequest action = new ApprovalActionRequest(request.comments());

        List<BulkApprovalFailure> failures = new ArrayList<>();
        int approved = 0;

        for (UUID requestId : request.requestIds()) {

            if (requestId == null) {
                continue;
            }

            try {
                gatePassRequestService.approve(requestId, action);
                approved++;

            } catch (RuntimeException exception) {
                /*
                 * The message is the one the API would have returned
                 * for this pass on its own - "already approved by a
                 * manager", "a rejected or cancelled gate pass
                 * cannot be approved" - so the screen can say
                 * exactly which ones did not go through and why.
                 */
                failures.add(new BulkApprovalFailure(
                        requestId,
                        exception.getMessage() == null
                                ? "Could not be signed"
                                : exception.getMessage()));
            }
        }

        return new BulkApprovalResponse(approved, failures.size(), failures);
    }
}
