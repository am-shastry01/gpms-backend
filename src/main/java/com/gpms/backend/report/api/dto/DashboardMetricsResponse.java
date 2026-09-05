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
        /**
         * Added so the admin dashboard can render its "rejected" and
         * "completed" tiles from one call. The app previously had to
         * pull two extra pages of requests and count them itself.
         */
        long rejectedToday,
        long completedToday,
        List<VendorDispatchSummaryResponse> vendorDispatches
) {
}
