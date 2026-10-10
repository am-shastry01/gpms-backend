package com.gpms.backend.auth.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Step one of a forgotten password: the address to send a code to.
 *
 * The email address is the identity in this system, so it is the only
 * thing asked for. Whether it has an account is deliberately not
 * revealed by the answer.
 */
public record SendOtpRequest(
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email
) {
}
