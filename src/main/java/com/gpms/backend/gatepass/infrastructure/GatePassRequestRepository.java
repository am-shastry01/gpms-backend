package com.gpms.backend.gatepass.infrastructure;

import com.gpms.backend.gatepass.domain.GatePassRequest;
import com.gpms.backend.gatepass.domain.GatePassStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatePassRequestRepository
        extends JpaRepository<GatePassRequest, UUID>, JpaSpecificationExecutor<GatePassRequest> {

    Optional<GatePassRequest> findByIdAndDeletedFalse(UUID id);

    Optional<GatePassRequest> findByGatePassNumberIgnoreCaseAndDeletedFalse(String gatePassNumber);


    Page<GatePassRequest> findAllByRequestedByIdAndDeletedFalse(UUID requestedById, Pageable pageable);

    long countByStatusAndDeletedFalse(GatePassStatus status);

    @Query("""
            select count(g) from GatePassRequest g
            where g.deleted = false
              and g.approvalTime >= :start
              and g.approvalTime < :end
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            """)
    long countApprovedBetween(@Param("start") Instant start, @Param("end") Instant end, @Param("warehouseId") UUID warehouseId);

    /**
     * Gate passes actually issued in the window.
     *
     * This replaces the old "exited" count, which was driven by a
     * timestamp only the security guard could write. With no guard
     * in the flow, the honest measure of a day's dispatch activity
     * is how many passes were generated.
     */
    @Query("""
            select count(g) from GatePassRequest g
            where g.deleted = false
              and g.gatePassNumber is not null
              and g.createdAt >= :start
              and g.createdAt < :end
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            """)
    long countIssuedBetween(@Param("start") Instant start, @Param("end") Instant end, @Param("warehouseId") UUID warehouseId);

    @Query("""
            select v.name, count(g)
            from GatePassRequest g
            join g.vendor v
            where g.deleted = false
              and g.dispatchDate between :fromDate and :toDate
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            group by v.name
            order by count(g) desc
            """)
    List<Object[]> countDispatchesByVendor(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("warehouseId") UUID warehouseId
    );

    @Query("""
            select count(g) from GatePassRequest g
            where g.deleted = false
              and g.status = :status
              and g.updatedAt >= :start
              and g.updatedAt < :end
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            """)
    long countByStatusBetween(
            @Param("status") GatePassStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("warehouseId") UUID warehouseId
    );

    @Query("""
            select count(g) from GatePassRequest g
            where g.deleted = false
              and g.status in :statuses
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            """)
    long countByStatusesAndWarehouse(
            @Param("statuses") List<GatePassStatus> statuses,
            @Param("warehouseId") UUID warehouseId
    );

    /*
     * The two authorisation queues.
     *
     * Deliberately keyed on "has MY role signed yet", not on status.
     * A pass is APPROVED the moment either approver signs, so a
     * status-based queue would drop it the instant the manager
     * signed and leave the admin with no way to find it - even
     * though their own signature block is still blank and the
     * document is waiting on them for audit.
     *
     * Rejected and cancelled passes are excluded: there is nothing
     * left to authorise on a pass that went nowhere.
     */
    @Query("""
            select g from GatePassRequest g
            where g.deleted = false
              and g.status not in :excluded
              and g.employeeApprovedAt is null
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            order by g.createdAt desc
            """)
    List<GatePassRequest> findAwaitingEmployeeSignature(
            @Param("excluded") List<GatePassStatus> excluded,
            @Param("warehouseId") UUID warehouseId
    );

    @Query("""
            select g from GatePassRequest g
            where g.deleted = false
              and g.status not in :excluded
              and g.managerApprovedAt is null
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            order by g.createdAt desc
            """)
    List<GatePassRequest> findAwaitingManagerSignature(
            @Param("excluded") List<GatePassStatus> excluded,
            @Param("warehouseId") UUID warehouseId
    );

    @Query("""
            select g from GatePassRequest g
            where g.deleted = false
              and g.status not in :excluded
              and g.adminApprovedAt is null
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            order by g.createdAt desc
            """)
    List<GatePassRequest> findAwaitingAdminSignature(
            @Param("excluded") List<GatePassStatus> excluded,
            @Param("warehouseId") UUID warehouseId
    );

    @Query("""
            select count(g) from GatePassRequest g
            where g.deleted = false
              and g.requestedBy.id = :userId
              and g.status in :statuses
            """)
    long countByRequesterAndStatuses(
            @Param("userId") UUID userId,
            @Param("statuses") List<GatePassStatus> statuses
    );
}
