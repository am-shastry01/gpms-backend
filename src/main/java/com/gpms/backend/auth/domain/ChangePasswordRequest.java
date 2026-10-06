package com.gpms.backend.auth.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * What a person sends to replace their own password.
 *
 * The current password is required even when the account is still on
 * the temporary one an admin issued: it is the only proof that the
 * person holding the phone is the person the password was handed to.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        String newPassword
) {
}
