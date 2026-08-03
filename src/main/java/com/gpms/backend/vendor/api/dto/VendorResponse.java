package com.gpms.backend.vendor.api.dto;

import java.time.Instant;
import java.util.UUID;

public record VendorResponse(
        UUID id,
        String code,
        String name,
        String contactPerson,
        String phoneNumber,
        String email,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
