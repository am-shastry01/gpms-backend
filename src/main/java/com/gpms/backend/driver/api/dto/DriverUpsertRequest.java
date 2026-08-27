package com.gpms.backend.driver.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverUpsertRequest(
        @NotBlank(message = "Driver name is required")
        String name,
        @NotBlank(message = "Driver mobile number is required")
        String mobileNumber,
        String licenseNumber,
        Boolean active
) {
}
