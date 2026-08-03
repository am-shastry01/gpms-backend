package com.gpms.backend.report.api;

import com.gpms.backend.report.api.dto.DashboardMetricsResponse;
import com.gpms.backend.report.application.ReportService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public DashboardMetricsResponse dashboard(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) UUID warehouseId
    ) {
        return reportService.dashboard(date, warehouseId);
    }
}
