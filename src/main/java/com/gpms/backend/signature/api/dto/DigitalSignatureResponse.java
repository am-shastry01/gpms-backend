package com.gpms.backend.signature.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DigitalSignatureResponse(
        UUID id,
        UUID userId,
        String userFullName,
        String fileName,
        String contentType,
        Long sizeBytes,
        Instant uploadedAt
) {
}
