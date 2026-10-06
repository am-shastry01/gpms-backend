package com.gpms.backend.report.application;

import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.gatepass.application.GatePassSpecifications;
import com.gpms.backend.gatepass.domain.GatePassStatus;
import com.gpms.backend.gatepass.infrastructure.GatePassRequestRepository;
import com.gpms.backend.report.api.dto.DashboardMetricsResponse;
import com.gpms.backend.report.api.dto.MySummaryResponse;
import com.gpms.backend.report.api.dto.VendorDispatchSummaryResponse;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final GatePassRequestRepository gatePassRequestRepository;
    private final CurrentUserService currentUserService;

    /*
     * The zone a working day is measured in.
     *
     * "Today" used to be a UTC day, while the app asks for the
     * device's LOCAL date - so in India the day ran 05:30 to 05:30
     * and a pass raised at 03:00 was counted against yesterday.
     * The warehouse decides when its day starts, not the server's
     * clock, so it is configurable and defaults to where the
     * warehouse is.
     */
    private final ZoneId reportingZone;

    public ReportService(
            GatePassRequestRepository gatePassRequestRepository,
            CurrentUserService currentUserService,
            @Value("${gpms.reporting.zone:Asia/Kolkata}") String reportingZone
    ) {
        this.gatePassRequestRepository = gatePassRequestRepository;
        this.currentUserService = currentUserService;
        this.reportingZone = ZoneId.of(reportingZone);
    }

    /**
     * Approval workload for the admin/manager dashboard.
     */
    public DashboardMetricsResponse dashboard(LocalDate date, UUID warehouseId) {
        User currentUser = currentUserService.requireCurrentUser();
        UUID effectiveWarehouseId = resolveWarehouseId(currentUser, warehouseId);

        /*
         * Every count below is fenced by this. A null warehouse means
         * "all of them", which for an admin used to mean every client's
         * - so the company id is not optional anywhere in here.
         */
        UUID companyId = currentUser.companyId();
        LocalDate effectiveDate = date == null ? LocalDate.now(reportingZone) : date;

        /*
         * Midnight to midnight in the warehouse's own zone, then
         * converted - so the window lines up with the date the app
         * asked for instead of being shifted by the offset.
         */
        Instant start = effectiveDate.atStartOfDay(reportingZone).toInstant();
        Instant end = effectiveDate.plusDays(1).atStartOfDay(reportingZone).toInstant();

        /*
         * An account that is neither an admin nor posted to a site has
         * no figures of its own: a null warehouse reads as "all of
         * them", which would have shown it the whole company's numbers.
         */
        if (!hasRole(currentUser, "ADMIN") && effectiveWarehouseId == null) {
            return new DashboardMetricsResponse(
                    effectiveDate, null, 0, 0, 0, 0, 0, List.of());
        }

        long pendingApprovals = gatePassRequestRepository.count(
                Specification.where(GatePassSpecifications.notDeleted())
                        .and(GatePassSpecifications.hasStatus(GatePassStatus.PENDING))
                        .and(GatePassSpecifications.companyEquals(companyId))
                        .and(GatePassSpecifications.warehouseEquals(effectiveWarehouseId))
        );

        List<VendorDispatchSummaryResponse> vendorDispatches = gatePassRequestRepository
                .countDispatchesByVendor(
                        effectiveDate.withDayOfMonth(1), effectiveDate,
                        companyId, effectiveWarehouseId)
                .stream()
                .map(row -> new VendorDispatchSummaryResponse((String) row[0], (Long) row[1]))
                .toList();

        return new DashboardMetricsResponse(
                effectiveDate,
                effectiveWarehouseId,
                pendingApprovals,
                gatePassRequestRepository.countApprovedBetween(start, end, companyId, effectiveWarehouseId),
                gatePassRequestRepository.countIssuedBetween(start, end, companyId, effectiveWarehouseId),
                gatePassRequestRepository.countByStatusBetween(
                        GatePassStatus.REJECTED, start, end, companyId, effectiveWarehouseId),
                gatePassRequestRepository.countByStatusBetween(
                        GatePassStatus.COMPLETED, start, end, companyId, effectiveWarehouseId),
                vendorDispatches
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
                        userId, List.of(GatePassStatus.APPROVED, GatePassStatus.GATE_GENERATED)),
                gatePassRequestRepository.countByRequesterAndStatuses(
                        userId, List.of(GatePassStatus.REJECTED, GatePassStatus.CANCELLED)),
                gatePassRequestRepository.countByRequesterAndStatuses(
                        userId, List.of(GatePassStatus.COMPLETED))
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
