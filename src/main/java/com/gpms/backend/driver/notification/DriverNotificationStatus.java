package com.gpms.backend.driver.notification;

public enum DriverNotificationStatus {
    SENT,
    FAILED,
    /** No mobile number on file, or SMS switched off in config. */
    SKIPPED
}
