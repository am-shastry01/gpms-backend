package com.gpms.backend.sms;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The default gateway: writes the message to the log instead of
 * sending it.
 *
 * This is what makes the whole flow testable with no provider
 * account and no cost - the driver link appears in the backend
 * console, and you can paste it into a browser to see exactly what
 * the driver would see.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.sms",
        name = "provider",
        havingValue = "logging",
        matchIfMissing = true
)
public class LoggingSmsGateway implements SmsGateway {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public SmsSendResult send(String mobileNumber, String message) {

        LOGGER.info(
                """

                        ===================== SMS (not actually sent) =====================
                        To      : {}
                        Message : {}
                        ===================================================================
                        Set app.sms.provider=twilio with credentials to send for real.
                        """,
                mobileNumber,
                message
        );

        return SmsSendResult.ok("logged-" + UUID.randomUUID());
    }

    @Override
    public String providerName() {
        return "logging";
    }
}
