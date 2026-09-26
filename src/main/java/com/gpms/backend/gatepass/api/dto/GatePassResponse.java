package com.gpms.backend.gatepass.api.dto;

import com.gpms.backend.gatepass.domain.GatePassStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GatePassResponse(
        UUID id,
        String requestNumber,
        String gatePassNumber,
        GatePassStatus status,
        UUID warehouseId,
        String warehouseName,
        UUID vendorId,
        String vendorName,
        UUID driverId,
        String driverName,
        String driverMobile,
        UUID vehicleId,
        String truckNumber,
        String vehicleType,
        Integer packageCount,
        String packageDescription,
        String destination,
        LocalDate dispatchDate,
        String transportName,
        String invoiceNumber,
        LocalDate invoiceDate,
        String remarks,
        String managerComments,
        UUID requestedById,
        String requestedByName,
        UUID approvedById,
        String approvedByName,
        Instant approvalTime,

        /*
         * The three signature blocks on the printed pass.
         *
         * Whoever raised the pass has their own block filled the
         * moment it exists; the other two fill in as and when those
         * roles sign, on whatever date that happens.
         */
        UUID employeeApprovedById,
        String employeeApprovedByName,
        Instant employeeApprovedAt,
        UUID managerApprovedById,
        String managerApprovedByName,
        Instant managerApprovedAt,
        UUID adminApprovedById,
        String adminApprovedByName,
        Instant adminApprovedAt,

        List<GatePassItemResponse> items,
        List<ApprovalTrailResponse> approvals,
        List<AttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {
}
