package com.gpms.backend.report.application;

import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.gatepass.application.GatePassSpecifications;
import com.gpms.backend.gatepass.domain.GatePassStatus;
import com.gpms.backend.gatepass.infrastructure.GatePassRequestRepository;
import com.gpms.backend.report.api.dto.DashboardMetricsResponse;
import com.gpms.backend.report.api.dto.GateDashboardResponse;
import com.gpms.backend.report.api.dto.MySummaryResponse;
import com.gpms.backend.report.api.dto.VendorDispatchSummaryResponse;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
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

    /**
     * Approval workload for the admin/manager dashboard.
     */
    public DashboardMetricsResponse dashboard(LocalDate date, UUID warehouseId) {
        User currentUser = currentUserService.requireCurrentUser();
        UUID effectiveWarehouseId = resolveWarehouseId(currentUser, warehouseId);
        LocalDate effectiveDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        Instant start = effectiveDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = effectiveDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long pendingApprovals = gatePassRequestRepository.count(
                Specification.where(GatePassSpecifications.notDeleted())
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
                gatePassRequestRepository.countByStatusBetween(
                        GatePassStatus.REJECTED, start, end, effectiveWarehouseId),
                gatePassRequestRepository.countByStatusBetween(
                        GatePassStatus.COMPLETED, start, end, effectiveWarehouseId),
                vendorDispatches
        );
    }

    /**
     * Gate movement for the security guard's dashboard.
     */
    public GateDashboardResponse gateDashboard(LocalDate date) {
        User currentUser = currentUserService.requireCurrentUser();
        UUID effectiveWarehouseId = resolveWarehouseId(currentUser, null);
        LocalDate effectiveDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        Instant start = effectiveDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = effectiveDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long awaitingEntry = gatePassRequestRepository.countByStatusesAndWarehouse(
                List.of(GatePassStatus.APPROVED, GatePassStatus.GATE_GENERATED),
                effectiveWarehouseId
        );

        long insidePremises = gatePassRequestRepository.countByStatusesAndWarehouse(
                List.of(GatePassStatus.ENTERED),
                effectiveWarehouseId
        );

        return new GateDashboardResponse(
                effectiveDate,
                effectiveWarehouseId,
                awaitingEntry,
                insidePremises,
                gatePassRequestRepository.countEnteredBetween(start, end, effectiveWarehouseId),
                gatePassRequestRepository.countExitedBetween(start, end, effectiveWarehouseId)
        );
    }

    /**
     * The signed-in user's own request counts, for the employee
     * dashboard. Any authenticated user may call this - it only ever
     * reports on their own requests.
     */
    public MySummaryResponse mySummary() {
        User currentUser = currentUserService.requireCurrentUser();
        UUID userId = currentUser.getId();

        return new MySummaryResponse(
                gatePassRequestRepository.countByRequesterAndStatuses(
                        userId, List.of(GatePassStatus.DRAFT, GatePassStatus.PENDING)),
                gatePassRequestRepository.countByRequesterAndStatuses(
                        userId, List.of(GatePassStatus.APPROVED, GatePassStatus.GATE_GENERATED,
                                GatePassStatus.ENTERED)),
                gatePassRequestRepository.countByRequesterAndStatuses(
                        userId, List.of(GatePassStatus.REJECTED, GatePassStatus.CANCELLED)),
                gatePassRequestRepository.countByRequesterAndStatuses(
                        userId, List.of(GatePassStatus.EXITED, GatePassStatus.COMPLETED))
        );
    }

    /**
     * An ADMIN may look across warehouses; everyone else is pinned to
     * their own. Users with no warehouse assigned fall through to
     * null, which the queries treat as "all warehouses" rather than
     * throwing a NullPointerException.
     */
    private UUID resolveWarehouseId(User user, UUID requestedWarehouseId) {
        if (hasRole(user, "ADMIN")) {
            return requestedWarehouseId;
        }
        return user.getWarehouse() == null ? null : user.getWarehouse().getId();
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().map(Role::getCode).anyMatch(roleCode::equalsIgnoreCase);
    }
}
