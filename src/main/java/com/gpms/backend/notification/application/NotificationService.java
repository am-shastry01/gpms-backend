package com.gpms.backend.notification.application;

import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import com.gpms.backend.notification.api.dto.NotificationResponse;
import com.gpms.backend.notification.domain.Notification;
import com.gpms.backend.notification.domain.NotificationChannel;
import com.gpms.backend.notification.domain.NotificationStatus;
import com.gpms.backend.notification.infrastructure.NotificationRepository;
import com.gpms.backend.user.domain.User;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final PushNotificationGateway pushNotificationGateway;
    private final CurrentUserService currentUserService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            PushNotificationGateway pushNotificationGateway,
            CurrentUserService currentUserService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.pushNotificationGateway = pushNotificationGateway;
        this.currentUserService = currentUserService;
    }

    public List<NotificationResponse> getMyNotifications() {
        return notificationRepository.findAllByRecipientUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        currentUserService.requireCurrentUser().getId()
                )
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public NotificationResponse markAsRead(UUID notificationId) {
        User currentUser = currentUserService.requireCurrentUser();
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipientUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    public void notifyUsers(List<User> recipients, GatePassRequest gatePassRequest, String title, String message) {
        Set<UUID> deduplicated = new HashSet<>();
        for (User recipient : recipients) {
            if (!deduplicated.add(recipient.getId())) {
                continue;
            }
            Notification notification = new Notification();
            notification.setRecipientUser(recipient);
            notification.setGatePassRequest(gatePassRequest);
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setTitle(title);
            notification.setMessage(message);
            boolean sent = pushNotificationGateway.send(recipient, title, message);
            notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);
        }
    }
}
