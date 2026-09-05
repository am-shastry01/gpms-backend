package com.gpms.backend.driver.notification;

import com.gpms.backend.config.AppProperties;
import com.gpms.backend.config.SmsProperties;
import com.gpms.backend.gatepass.application.GatePassTokenService;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import com.gpms.backend.sms.SmsGateway;
import com.gpms.backend.sms.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Texts an approved gate pass to the truck driver.
 *
 * The driver has no account, so they get a link addressed by a random
 * token. Opening it shows a page with the QR the guard scans.
 */
@Service
public class DriverNotificationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DriverNotificationService.class);

    private final SmsGateway smsGateway;
    private final SmsProperties smsProperties;
    private final AppProperties appProperties;
    private final GatePassTokenService gatePassTokenService;
    private final DriverNotificationRepository driverNotificationRepository;

    public DriverNotificationService(
            SmsGateway smsGateway,
            SmsProperties smsProperties,
            AppProperties appProperties,
            GatePassTokenService gatePassTokenService,
            DriverNotificationRepository driverNotificationRepository
    ) {
        this.smsGateway = smsGateway;
        this.smsProperties = smsProperties;
        this.appProperties = appProperties;
        this.gatePassTokenService = gatePassTokenService;
        this.driverNotificationRepository = driverNotificationRepository;
    }

    /**
     * Called once a pass has been approved and a token issued.
     *
     * Never throws: a texting problem must not roll back an approval
     * that has already been decided. Failures are recorded instead,
     * so they can be seen and retried.
     */
    @Transactional
    public void sendGatePass(GatePassRequest gatePassRequest, String token) {

        String mobileNumber = gatePassRequest.getDriver() == null
                ? null
                : gatePassRequest.getDriver().getMobileNumber();

        String link = gatePassTokenService.buildDriverLink(
                appProperties.getPublicBaseUrl(),
                token
        );

        String message = buildMessage(gatePassRequest, link);

        if (!smsProperties.isEnabled()) {
            record(gatePassRequest, mobileNumber, message, link,
                    DriverNotificationStatus.SKIPPED, null,
                    "Driver SMS is switched off (app.sms.enabled=false)");
            return;
        }

        if (mobileNumber == null || mobileNumber.isBlank()) {
            record(gatePassRequest, null, message, link,
                    DriverNotificationStatus.SKIPPED, null,
                    "No mobile number on file for this driver");
            return;
        }

        try {

            SmsSendResult result = smsGateway.send(mobileNumber, message);

            record(
                    gatePassRequest,
                    mobileNumber,
                    message,
                    link,
                    result.success()
                            ? DriverNotificationStatus.SENT
                            : DriverNotificationStatus.FAILED,
                    result.providerMessageId(),
                    result.errorDetail()
            );

        } catch (Exception exception) {

            LOGGER.warn(
                    "Could not text the gate pass to {}: {}",
                    mobileNumber,
                    exception.getMessage()
            );

            record(gatePassRequest, mobileNumber, message, link,
                    DriverNotificationStatus.FAILED, null,
                    exception.getMessage());
        }
    }

    private String buildMessage(GatePassRequest gatePassRequest, String link) {

        String vehicle = gatePassRequest.getVehicle() == null
                ? ""
                : gatePassRequest.getVehicle().getRegistrationNumber();

        return "Gate Pass " + gatePassRequest.getGatePassNumber()
                + " approved for vehicle " + vehicle
                + ". Show this at the gate: " + link;
    }

    private void record(
            GatePassRequest gatePassRequest,
            String mobileNumber,
            String message,
            String link,
            DriverNotificationStatus status,
            String providerMessageId,
            String errorDetail
    ) {

        DriverNotification notification = new DriverNotification();
        notification.setGatePassRequest(gatePassRequest);
        notification.setDriver(gatePassRequest.getDriver());
        notification.setMobileNumber(
                mobileNumber == null ? "unknown" : mobileNumber);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setStatus(status);
        notification.setProvider(smsGateway.providerName());
        notification.setProviderMessageId(providerMessageId);
        notification.setErrorDetail(truncate(errorDetail));

        driverNotificationRepository.save(notification);
    }

    private String truncate(String value) {

        if (value == null) {
            return null;
        }

        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
