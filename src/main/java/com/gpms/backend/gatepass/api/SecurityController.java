package com.gpms.backend.gatepass.api;

import com.gpms.backend.gatepass.api.dto.GateActionRequest;
import com.gpms.backend.gatepass.api.dto.GatePassResponse;
import com.gpms.backend.gatepass.api.dto.PassVerificationRequest;
import com.gpms.backend.gatepass.api.dto.PassVerificationResponse;
import jakarta.validation.Valid;
import com.gpms.backend.gatepass.application.GatePassRequestService;
import com.gpms.backend.report.api.dto.GateDashboardResponse;
import com.gpms.backend.report.application.ReportService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
@PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
public class SecurityController {

    private final GatePassRequestService gatePassRequestService;
    private final ReportService reportService;

    public SecurityController(
            GatePassRequestService gatePassRequestService,
            ReportService reportService
    ) {
        this.gatePassRequestService = gatePassRequestService;
        this.reportService = reportService;
    }

    /**
     * Checks a QR scanned off a driver's phone.
     *
     * Answers with a verdict for every outcome - valid, already used,
     * expired, not approved, wrong warehouse - so the guard is told
     * what is wrong rather than just that something is.
     */
    @PostMapping("/verify")
    public PassVerificationResponse verify(
            @Valid @RequestBody PassVerificationRequest request
    ) {
        return gatePassRequestService.verifyScannedPass(
                request.token(),
                request.consume() != null && request.consume()
        );
    }

    /**
     * Records a truck arriving at the gate.
     */
    @PostMapping("/gate-pass-requests/{requestId}/entry")
    public GatePassResponse markEntry(
            @PathVariable UUID requestId,
            @RequestBody(required = false) GateActionRequest request
    ) {
        return gatePassRequestService.markEntry(requestId, request);
    }

    /**
     * Records a truck leaving.
     */
    @PostMapping("/gate-pass-requests/{requestId}/exit")
    public GatePassResponse markExit(
            @PathVariable UUID requestId,
            @RequestBody(required = false) GateActionRequest request
    ) {
        return gatePassRequestService.markExit(requestId, request);
    }

    /**
     * Resolves a gate pass from the number printed on the pass or
     * encoded in its QR code.
     */
    @GetMapping("/gate-pass-requests/by-number/{gatePassNumber}")
    public GatePassResponse getByNumber(@PathVariable String gatePassNumber) {
        return gatePassRequestService.getByGatePassNumber(gatePassNumber);
    }

    /**
     * Gate movement numbers for the guard's dashboard.
     *
     * Separate from /reports/dashboard, which is ADMIN/MANAGER only.
     */
    @GetMapping("/dashboard")
    public GateDashboardResponse dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reportService.gateDashboard(date);
    }
}
