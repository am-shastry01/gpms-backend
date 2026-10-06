package com.gpms.backend.auth.domain;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        UserContext user
) {
    public record UserContext(
            UUID id,
            String username,
            String fullName,
            String email,
            UUID warehouseId,
            String warehouseName,
            List<String> roles,

            /*
             * True while the account still carries the password an
             * admin issued. The app sends the person straight to the
             * "create a new password" screen when it is.
             */
            boolean mustChangePassword
    ) {
    }
}
