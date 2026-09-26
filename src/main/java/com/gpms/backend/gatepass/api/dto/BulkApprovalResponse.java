package com.gpms.backend.gatepass.api.dto;

import java.util.List;

/**
 * The outcome of signing a batch.
 *
 * Reports per-item results rather than a single success flag,
 * because a batch genuinely can be partly successful: one pass in a
 * selection may have been rejected, cancelled, or already signed by
 * this role while the screen was open. Collapsing that into "failed"
 * would hide the eight that did go through.
 */
public record BulkApprovalResponse(
        int approved,
        int failed,
        List<BulkApprovalFailure> failures
) {
}
