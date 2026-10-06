package com.gpms.backend.vendor.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VendorUpsertRequest(
        /*
         * Optional - derived from the name when absent, same as a
         * transport company's.
         */
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
