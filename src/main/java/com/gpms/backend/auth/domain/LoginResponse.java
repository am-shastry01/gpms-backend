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
            List<String> roles
    ) {
    }
}
