package com.gpms.backend.notification.application;

import com.gpms.backend.user.domain.User;

public interface PushNotificationGateway {

    boolean send(User recipient, String title, String message);
}
