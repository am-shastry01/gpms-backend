package com.gpms.backend.vehicletype.api.dto;

import java.time.Instant;
import java.util.UUID;

public record VehicleTypeResponse(
        UUID id,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
