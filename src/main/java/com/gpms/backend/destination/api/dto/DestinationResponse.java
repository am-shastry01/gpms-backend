package com.gpms.backend.destination.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DestinationResponse(
        UUID id,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
