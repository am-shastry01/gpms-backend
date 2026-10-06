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

    /**
     * Matches nothing.
     *
     * For the case where the caller is entitled to see no gate passes at
     * all. Needed because a null warehouse filter means "every
     * warehouse", so an account with no warehouse of its own would
     * otherwise fall through to seeing all of them.
     */
    public static Specification<GatePassRequest> nothing() {
        return (root, query, builder) -> builder.disjunction();
    }

    public static Specification<GatePassRequest> notDeleted() {
        return (root, query, builder) -> builder.isFalse(root.get("deleted"));
    }

    public static Specification<GatePassRequest> hasStatus(GatePassStatus status) {
        return status == null ? null : (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    /*
     * The tenancy fence.
     *
     * A gate pass does not carry a company of its own - it belongs to
     * one through its warehouse - so this joins through to
     * warehouse.company. Returning null when the id is null would be a
     * trap here: a null company means "no company", and the only honest
     * answer to that is nothing at all, never everything.
     */
    public static Specification<GatePassRequest> companyEquals(UUID companyId) {
        return (root, query, builder) -> companyId == null
                ? builder.disjunction()
                : builder.equal(
                        root.get("warehouse").get("company").get("id"), companyId);
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
