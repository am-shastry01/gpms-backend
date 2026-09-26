package com.gpms.backend.gatepass.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BulkApprovalRequest(
        @NotEmpty(message = "Select at least one gate pass")
        List<UUID> requestIds,
        String comments
) {
}
