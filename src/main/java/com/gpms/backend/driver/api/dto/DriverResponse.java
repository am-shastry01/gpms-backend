package com.gpms.backend.driver.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DriverResponse(
        UUID id,
        String name,
        String mobileNumber,
        String licenseNumber,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
