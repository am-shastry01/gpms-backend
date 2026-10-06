package com.gpms.backend.company.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompanyProfileUpdateRequest(
        @NotBlank(message = "Company name is required")
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String phoneNumber,
        @Email(message = "Email is invalid")
        String email,
        String gstNumber,
        String website
) {
}
