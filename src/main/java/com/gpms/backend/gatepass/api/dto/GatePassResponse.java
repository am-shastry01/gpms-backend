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
        String remarks,
        String managerComments,
        String qrContent,
        UUID requestedById,
        String requestedByName,
        UUID approvedById,
        String approvedByName,
        Instant approvalTime,
        UUID exitedById,
        String exitedByName,
        Instant exitTime,
        UUID enteredById,
        String enteredByName,
        Instant entryTime,
        List<GatePassItemResponse> items,
        List<ApprovalTrailResponse> approvals,
        List<AttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {
}
