package com.gpms.backend.destination.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DestinationUpsertRequest(
        @NotBlank(message = "Destination name is required")
        String name,
        Boolean active
) {
}
