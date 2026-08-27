package com.gpms.backend.report.application;

import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.gatepass.application.GatePassSpecifications;
import com.gpms.backend.gatepass.domain.GatePassStatus;
import com.gpms.backend.gatepass.infrastructure.GatePassRequestRepository;
import com.gpms.backend.report.api.dto.DashboardMetricsResponse;
import com.gpms.backend.report.api.dto.VendorDispatchSummaryResponse;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final GatePassRequestRepository gatePassRequestRepository;
    private final CurrentUserService currentUserService;

    public ReportService(
            GatePassRequestRepository gatePassRequestRepository,
            CurrentUserService currentUserService
    ) {
        this.gatePassRequestRepository = gatePassRequestRepository;
        this.currentUserService = currentUserService;
    }

    public DashboardMetricsResponse dashboard(LocalDate date, UUID warehouseId) {
        User currentUser = currentUserService.requireCurrentUser();
        UUID effectiveWarehouseId = hasRole(currentUser, "ADMIN") ? warehouseId : currentUser.getWarehouse().getId();
        LocalDate effectiveDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        Instant start = effectiveDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = effectiveDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long pendingApprovals = gatePassRequestRepository.count(
                org.springframework.data.jpa.domain.Specification.where(GatePassSpecifications.notDeleted())
                        .and(GatePassSpecifications.hasStatus(GatePassStatus.PENDING))
                        .and(GatePassSpecifications.warehouseEquals(effectiveWarehouseId))
        );

        List<VendorDispatchSummaryResponse> vendorDispatches = gatePassRequestRepository
                .countDispatchesByVendor(effectiveDate.withDayOfMonth(1), effectiveDate, effectiveWarehouseId)
                .stream()
                .map(row -> new VendorDispatchSummaryResponse((String) row[0], (Long) row[1]))
                .toList();

        return new DashboardMetricsResponse(
                effectiveDate,
                effectiveWarehouseId,
                pendingApprovals,
                gatePassRequestRepository.countApprovedBetween(start, end, effectiveWarehouseId),
                gatePassRequestRepository.countExitedBetween(start, end, effectiveWarehouseId),
                vendorDispatches
        );
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().map(Role::getCode).anyMatch(roleCode::equalsIgnoreCase);
    }
}
