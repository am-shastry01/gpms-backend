package com.gpms.backend.driver.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverNotificationRepository
        extends JpaRepository<DriverNotification, UUID> {

    List<DriverNotification> findByGatePassRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID gatePassRequestId
    );
}
