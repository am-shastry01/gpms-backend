package com.gpms.backend.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * An admin handing out a fresh temporary password.
 *
 * It lands on the account as a must-change password, exactly like the
 * one issued when the account was created, so the person still ends up
 * choosing their own.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {
}
