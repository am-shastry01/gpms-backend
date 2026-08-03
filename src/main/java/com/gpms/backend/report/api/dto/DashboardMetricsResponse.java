package com.gpms.backend.report.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardMetricsResponse(
        LocalDate date,
        UUID warehouseId,
        long pendingApprovals,
        long approvedToday,
        long exitedToday,
        List<VendorDispatchSummaryResponse> vendorDispatches
) {
}
