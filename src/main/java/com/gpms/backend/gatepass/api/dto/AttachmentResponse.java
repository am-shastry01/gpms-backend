package com.gpms.backend.gatepass.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        String objectKey,
        String objectUrl,
        Long sizeBytes,
        Instant createdAt
) {
}
