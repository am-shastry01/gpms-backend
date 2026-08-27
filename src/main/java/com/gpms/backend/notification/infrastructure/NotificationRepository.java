package com.gpms.backend.notification.infrastructure;

import com.gpms.backend.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByRecipientUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID recipientUserId);

    Optional<Notification> findByIdAndDeletedFalse(UUID id);
}
