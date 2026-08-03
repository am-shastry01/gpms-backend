package com.gpms.backend.vendor.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VendorUpsertRequest(
        @NotBlank(message = "Vendor code is required")
        String code,
        @NotBlank(message = "Vendor name is required")
        String name,
        String contactPerson,
        String phoneNumber,
        @Email(message = "Email is invalid")
        String email,
        Boolean active
) {
}
