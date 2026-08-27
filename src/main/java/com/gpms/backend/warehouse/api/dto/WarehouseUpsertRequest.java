package com.gpms.backend.warehouse.api.dto;

import jakarta.validation.constraints.NotBlank;

public record WarehouseUpsertRequest(
        @NotBlank(message = "Warehouse code is required")
        String code,
        @NotBlank(message = "Warehouse name is required")
        String name,
        String location,
        String addressLine,
        String city,
        String state,
        String country,
        String timezone,
        Boolean active
) {
}
