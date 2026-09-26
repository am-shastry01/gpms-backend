package com.gpms.backend.transport.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TransportResponse(
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
