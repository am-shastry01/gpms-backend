package com.gpms.backend.gatepass.api.dto;

import java.util.UUID;

/** One gate pass in a batch that could not be signed, and why. */
public record BulkApprovalFailure(
        UUID requestId,
        String reason
) {
}
