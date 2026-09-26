package com.gpms.backend.vehicletype.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleTypeUpsertRequest(
        @NotBlank(message = "Vehicle type name is required")
        String name,
        Boolean active
) {
}
