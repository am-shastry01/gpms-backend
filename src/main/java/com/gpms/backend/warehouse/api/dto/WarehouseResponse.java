package com.gpms.backend.warehouse.api.dto;

import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        String code,
        String name,
        String location,
        String addressLine,
        String city,
        String state,
        String country,
        String timezone,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
