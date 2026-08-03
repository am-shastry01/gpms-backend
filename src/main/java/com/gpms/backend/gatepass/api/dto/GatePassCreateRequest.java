package com.gpms.backend.gatepass.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GatePassCreateRequest(
        UUID warehouseId,
        @NotNull(message = "Vendor is required")
        UUID vendorId,
        UUID driverId,
        String driverName,
        String driverMobile,
        String driverLicenseNumber,
        UUID vehicleId,
        String truckNumber,
        String vehicleType,
        @Positive(message = "Package count must be greater than zero")
        Integer packageCount,
        @NotBlank(message = "Package description is required")
        String packageDescription,
        @NotBlank(message = "Destination is required")
        String destination,
        @NotNull(message = "Dispatch date is required")
        LocalDate dispatchDate,
        String remarks,
        @Valid
        List<GatePassItemRequest> items
) {
}
