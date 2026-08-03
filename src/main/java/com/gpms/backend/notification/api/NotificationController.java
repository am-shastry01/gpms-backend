package com.gpms.backend.notification.api;

import com.gpms.backend.notification.api.dto.NotificationResponse;
import com.gpms.backend.notification.application.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getMyNotifications() {
        return notificationService.getMyNotifications();
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable UUID notificationId) {
        return notificationService.markAsRead(notificationId);
    }
}
