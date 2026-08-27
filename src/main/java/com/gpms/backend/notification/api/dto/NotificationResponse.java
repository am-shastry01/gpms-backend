package com.gpms.backend.notification.api.dto;

import com.gpms.backend.notification.domain.NotificationChannel;
import com.gpms.backend.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID gatePassRequestId,
        String title,
        String message,
        NotificationChannel channel,
        NotificationStatus status,
        Instant sentAt,
        Instant readAt,
        Instant createdAt
) {
}
