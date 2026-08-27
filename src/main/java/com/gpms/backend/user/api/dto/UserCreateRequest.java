package com.gpms.backend.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record UserCreateRequest(
        @NotBlank(message = "Username is required")
        String username,
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Password is required")
        String password,
        @NotBlank(message = "Full name is required")
        String fullName,
        String mobileNumber,
        UUID warehouseId,
        @NotEmpty(message = "At least one role is required")
        List<String> roles,
        Boolean active
) {
}
