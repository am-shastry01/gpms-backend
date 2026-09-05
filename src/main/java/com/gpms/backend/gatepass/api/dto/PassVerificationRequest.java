package com.gpms.backend.gatepass.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PassVerificationRequest(

        @NotBlank(message = "Scanned code is required")
        String token,

        /**
         * When true the token is burned so the same QR cannot admit a
         * second truck. The app sends false for a look-only scan.
         */
        Boolean consume
) {
}
