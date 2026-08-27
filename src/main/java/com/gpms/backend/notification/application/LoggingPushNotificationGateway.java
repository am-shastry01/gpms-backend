package com.gpms.backend.notification.application;

import com.gpms.backend.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPushNotificationGateway implements PushNotificationGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPushNotificationGateway.class);

    @Override
    public boolean send(User recipient, String title, String message) {
        LOGGER.info("Notification dispatched to user={} title={} message={}", recipient.getUsername(), title, message);
        return true;
    }
}
