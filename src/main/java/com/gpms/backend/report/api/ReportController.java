package com.gpms.backend.report.api;

import com.gpms.backend.report.api.dto.DashboardMetricsResponse;
import com.gpms.backend.report.api.dto.MySummaryResponse;
import com.gpms.backend.report.application.ReportService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Approval workload. Restricted to the roles that act on it.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public DashboardMetricsResponse dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID warehouseId
    ) {
        return reportService.dashboard(date, warehouseId);
    }

    /**
     * The caller's own request counts.
     *
     * Open to any authenticated user: it reports only on requests
     * they raised themselves, so there is nothing to restrict.
     */
    @GetMapping("/my-summary")
    @PreAuthorize("isAuthenticated()")
    public MySummaryResponse mySummary() {
        return reportService.mySummary();
    }
}
