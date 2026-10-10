package com.gpms.backend.auth.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Step two of a forgotten password: the code that was emailed, and the
 * password to set with it.
 *
 * The code and the new password travel together on purpose. Verifying
 * the code in its own call would leave a proven-but-unspent code
 * sitting between two requests, which is one more thing to replay; here
 * the code is checked and used in the same transaction, and is deleted
 * before the reply is written.
 */
public record ForgotPasswordRequest(
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "The code is required")
        @Pattern(regexp = "\\d{6}", message = "The code is 6 digits")
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        String newPassword
) {
}
