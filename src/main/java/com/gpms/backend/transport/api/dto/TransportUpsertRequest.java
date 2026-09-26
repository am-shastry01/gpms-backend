package com.gpms.backend.transport.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TransportUpsertRequest(
        @NotBlank(message = "Transport code is required")
        String code,
        @NotBlank(message = "Transport name is required")
        String name,
        String contactPerson,
        String phoneNumber,
        @Email(message = "Email is invalid")
        String email,
        Boolean active
) {
}
