package com.gpms.backend.vehicle.api.dto;

import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String registrationNumber,
        String vehicleType,
        String capacity,
        boolean active,
        UUID vendorId,
        String vendorName,
        Instant createdAt,
        Instant updatedAt
) {
}
