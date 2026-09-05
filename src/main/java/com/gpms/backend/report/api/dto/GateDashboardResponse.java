package com.gpms.backend.report.api.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Numbers for the security guard's dashboard.
 *
 * Deliberately separate from DashboardMetricsResponse: that one is
 * restricted to ADMIN and MANAGER, so a SECURITY user calling it
 * gets a 403. This one is reachable by security staff and only
 * exposes gate movement, not approval workload.
 */
public record GateDashboardResponse(
        LocalDate date,
        UUID warehouseId,
        long awaitingEntry,
        long insidePremises,
        long entriesToday,
        long exitsToday
) {
}
