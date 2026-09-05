package com.gpms.backend.sms;

public record SmsSendResult(
        boolean success,
        String providerMessageId,
        String errorDetail
) {

    public static SmsSendResult ok(String providerMessageId) {
        return new SmsSendResult(true, providerMessageId, null);
    }

    public static SmsSendResult failed(String errorDetail) {
        return new SmsSendResult(false, null, errorDetail);
    }
}
