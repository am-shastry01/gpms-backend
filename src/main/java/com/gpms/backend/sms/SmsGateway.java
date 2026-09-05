package com.gpms.backend.sms;

/**
 * How an SMS leaves the system.
 *
 * Mirrors the existing PushNotificationGateway pattern: one
 * interface, a logging implementation that always works, and a real
 * provider swapped in by configuration.
 */
public interface SmsGateway {

    SmsSendResult send(String mobileNumber, String message);

    /** Name recorded against the delivery log. */
    String providerName();
}
