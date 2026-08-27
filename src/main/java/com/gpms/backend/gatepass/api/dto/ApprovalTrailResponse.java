package com.gpms.backend.gatepass.api.dto;

import com.gpms.backend.gatepass.domain.ApprovalAction;
import java.time.Instant;
import java.util.UUID;

public record ApprovalTrailResponse(
        UUID id,
        ApprovalAction action,
        String comments,
        UUID managerId,
        String managerName,
        Instant actionTime
) {
}
