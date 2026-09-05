package com.gpms.backend.sms;

import com.gpms.backend.config.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Real SMS delivery through Twilio.
 *
 * Deliberately calls Twilio's REST API with the RestClient already in
 * Spring rather than pulling in the Twilio SDK, so activating this
 * adds no dependency to the build - only configuration:
 *
 *   app.sms.provider=twilio
 *   app.sms.twilio.account-sid=AC...
 *   app.sms.twilio.auth-token=...
 *   app.sms.twilio.from-number=+1...
 *
 * Sending to Indian numbers additionally requires a DLT-registered
 * sender ID and template; without that the carrier drops the message
 * even though Twilio reports it as accepted.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.sms",
        name = "provider",
        havingValue = "twilio"
)
public class TwilioSmsGateway implements SmsGateway {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TwilioSmsGateway.class);

    private static final String API_ROOT = "https://api.twilio.com/2010-04-01";

    private final SmsProperties properties;
    private final RestClient restClient;

    public TwilioSmsGateway(SmsProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(API_ROOT)
                .build();
    }

    @Override
    public SmsSendResult send(String mobileNumber, String message) {

        SmsProperties.Twilio twilio = properties.getTwilio();

        if (twilio.getAccountSid() == null || twilio.getAccountSid().isBlank()
                || twilio.getAuthToken() == null || twilio.getAuthToken().isBlank()
                || twilio.getFromNumber() == null || twilio.getFromNumber().isBlank()) {

            return SmsSendResult.failed(
                    "Twilio is selected but account-sid, auth-token or "
                            + "from-number is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", mobileNumber);
        form.add("From", twilio.getFromNumber());
        form.add("Body", message);

        try {

            String response = restClient.post()
                    .uri("/Accounts/{sid}/Messages.json", twilio.getAccountSid())
                    .headers(headers -> headers.setBasicAuth(
                            twilio.getAccountSid(),
                            twilio.getAuthToken()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            LOGGER.info("SMS accepted by Twilio for {}", mobileNumber);

            return SmsSendResult.ok(extractSid(response));

        } catch (Exception exception) {

            LOGGER.warn(
                    "Twilio rejected the SMS for {}: {}",
                    mobileNumber,
                    exception.getMessage()
            );

            return SmsSendResult.failed(exception.getMessage());
        }
    }

    /*
     * Pulls "sid" out of the JSON response without dragging in a
     * parser - the value is only ever stored for traceability.
     */
    private String extractSid(String response) {

        if (response == null) {
            return null;
        }

        int keyIndex = response.indexOf("\"sid\"");

        if (keyIndex < 0) {
            return null;
        }

        int firstQuote = response.indexOf('"', response.indexOf(':', keyIndex));

        if (firstQuote < 0) {
            return null;
        }

        int lastQuote = response.indexOf('"', firstQuote + 1);

        return lastQuote < 0 ? null : response.substring(firstQuote + 1, lastQuote);
    }

    @Override
    public String providerName() {
        return "twilio";
    }
}
