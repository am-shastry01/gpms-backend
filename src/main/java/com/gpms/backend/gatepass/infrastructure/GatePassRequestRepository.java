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

    @Query("""
            select count(g) from GatePassRequest g
            where g.deleted = false
              and g.exitTime >= :start
              and g.exitTime < :end
              and (:warehouseId is null or g.warehouse.id = :warehouseId)
            """)
    long countExitedBetween(@Param("start") Instant start, @Param("end") Instant end, @Param("warehouseId") UUID warehouseId);

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
}
