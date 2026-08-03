package com.gpms.backend.gatepass.application;

import com.gpms.backend.gatepass.domain.GatePassRequest;
import com.gpms.backend.gatepass.domain.GatePassStatus;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class GatePassSpecifications {

    private GatePassSpecifications() {
    }

    public static Specification<GatePassRequest> notDeleted() {
        return (root, query, builder) -> builder.isFalse(root.get("deleted"));
    }

    public static Specification<GatePassRequest> hasStatus(GatePassStatus status) {
        return status == null ? null : (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<GatePassRequest> warehouseEquals(UUID warehouseId) {
        return warehouseId == null
                ? null
                : (root, query, builder) -> builder.equal(root.get("warehouse").get("id"), warehouseId);
    }

    public static Specification<GatePassRequest> requestedByEquals(UUID userId) {
        return userId == null
                ? null
                : (root, query, builder) -> builder.equal(root.get("requestedBy").get("id"), userId);
    }

    public static Specification<GatePassRequest> dispatchDateBetween(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return null;
        }
        return (root, query, builder) -> {
            if (fromDate != null && toDate != null) {
                return builder.between(root.get("dispatchDate"), fromDate, toDate);
            }
            if (fromDate != null) {
                return builder.greaterThanOrEqualTo(root.get("dispatchDate"), fromDate);
            }
            return builder.lessThanOrEqualTo(root.get("dispatchDate"), toDate);
        };
    }

    public static Specification<GatePassRequest> textSearch(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return null;
        }
        String likeQuery = "%" + queryText.trim().toLowerCase() + "%";
        return (root, query, builder) -> {
            var vendorJoin = root.join("vendor", JoinType.LEFT);
            var driverJoin = root.join("driver", JoinType.LEFT);
            var vehicleJoin = root.join("vehicle", JoinType.LEFT);
            return builder.or(
                    builder.like(builder.lower(root.get("requestNumber")), likeQuery),
                    builder.like(builder.lower(root.get("gatePassNumber")), likeQuery),
                    builder.like(builder.lower(vendorJoin.get("name")), likeQuery),
                    builder.like(builder.lower(driverJoin.get("name")), likeQuery),
                    builder.like(builder.lower(driverJoin.get("mobileNumber")), likeQuery),
                    builder.like(builder.lower(vehicleJoin.get("registrationNumber")), likeQuery),
                    builder.like(builder.lower(root.get("destination")), likeQuery)
            );
        };
    }
}
