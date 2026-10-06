package com.gpms.backend.transport.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TransportUpsertRequest(
        /*
         * Optional. Left out, it is derived from the name - a code is an
         * internal handle, and asking an admin to invent one before they
         * can add "Ekart" is how a simple form starts refusing input.
         */
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
