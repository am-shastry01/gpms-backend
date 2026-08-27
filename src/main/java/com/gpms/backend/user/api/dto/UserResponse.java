package com.gpms.backend.user.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String mobileNumber,
        boolean active,
        UUID warehouseId,
        String warehouseName,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
