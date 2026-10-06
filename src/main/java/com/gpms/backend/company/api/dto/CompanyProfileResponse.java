package com.gpms.backend.company.api.dto;

import java.util.UUID;

public record CompanyProfileResponse(
        UUID id,
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String phoneNumber,
        String email,
        String gstNumber,
        String website,
        boolean hasLogo
) {
}
