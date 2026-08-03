package com.gpms.backend.vehicle.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record VehicleUpsertRequest(
        @NotBlank(message = "Registration number is required")
        String registrationNumber,
        @NotBlank(message = "Vehicle type is required")
        String vehicleType,
        String capacity,
        UUID vendorId,
        Boolean active
) {
}
