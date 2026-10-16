package com.gpms.backend.auth.application;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sends the forgotten-password code through Zoho's HTTP mail API -
 * either ZeptoMail or CPaaS Email, which take the same request.
 *
 * NOT SMTP, and that is the whole point. This ran on Zoho's SMTP
 * first and failed in production with a connection timeout to
 * smtp.zoho.in:587 - Render's free instances block outbound traffic
 * on the SMTP ports (25, 465, 587) to stop them being used as spam
 * relays, so the connection never left the container. No amount of
 * fixing the credentials would have helped.
 *
 * The HTTP API posts over 443, which nothing blocks, and it is a
 * transactional-mail service rather than a mailbox: it is
 * independent of whatever plan Zoho Mail is on, and its delivery
 * reputation is built for exactly this kind of message.
 *
 * A send that fails is logged and swallowed. The caller must not be
 * told whether the address exists - and it also must not be told
 * whether OUR mail service is having a bad afternoon, because both
 * answers come back through the same reply and one of them would leak
 * the other. The person sees the same "if that address has an
 * account, a code is on its way" either way, and an operator reading
 * the log sees exactly what went wrong.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log =
            LoggerFactory.getLogger(PasswordResetMailer.class);

    /** Zoho's own auth scheme - not Bearer, and not base64-encoded. */
    private static final String SCHEME = "Zoho-enczapikey";

    private final RestClient restClient;

    private final String endpoint;

    private final String token;

    private final String fromAddress;

    private final String fromName;

    private final int validityMinutes;

    public PasswordResetMailer(
            @Value("${app.mail.api.url:https://api.zeptomail.in/v1.1/email}")
            String endpoint,
            @Value("${app.mail.api.token:}") String token,
            @Value("${app.mail.from-address:}") String fromAddress,
            @Value("${app.mail.from-name:Gate Pass}") String fromName,
            @Value("${app.password-reset.validity-minutes:10}") int validityMinutes
    ) {
        this.endpoint = endpoint;
        this.token = token;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.validityMinutes = validityMinutes;

        /*
         * Short timeouts on purpose. This call sits inside the request
         * the person is waiting on, and a reset code that takes thirty
         * seconds to not arrive is worse than one that fails fast and
         * says so in the log.
         */
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public void sendCode(String email, String fullName, String code) {

        if (token == null || token.isBlank()
                || fromAddress == null || fromAddress.isBlank()) {
            /*
             * No mail service configured. On a developer machine that
             * is normal, and the code is printed instead so the flow
             * can be walked through end to end without one.
             */
            log.warn("Mail is not configured; password reset code for {} is {}",
                    email, code);
            return;
        }

        try {

            Map<String, Object> body = Map.of(
                    "from", Map.of(
                            "address", fromAddress,
                            "name", fromName),
                    "to", List.of(Map.of(
                            "email_address", Map.of(
                                    "address", email,
                                    "name", fullName == null || fullName.isBlank()
                                            ? email : fullName))),
                    "subject", "Your Gate Pass password reset code",
                    "textbody", body(fullName, code));

            restClient.post()
                    .uri(endpoint)
                    .header("Authorization", authorization())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password reset code sent to {}", email);

        } catch (Exception failure) {
            /*
             * Logged with the address but never with the code: a log
             * file is not a place to keep live credentials.
             */
            log.error("Could not send the password reset code to {}: {}",
                    email, failure.getMessage());
        }
    }

    /*
     * The Authorization header, tolerant of how the token was copied.
     *
     * Zoho's console displays the key as "Zoho-enczapikey wSsVR..." -
     * prefix included - so copying it straight off that screen gives
     * you a string that already carries the scheme. Prepending it
     * again produces "Zoho-enczapikey Zoho-enczapikey wSsVR..." and a
     * 401 that says nothing about why.
     *
     * Rather than depend on whoever sets the variable trimming it
     * correctly, the prefix is added only when it is missing. Both
     * forms work, which is the point.
     */
    private String authorization() {

        String trimmed = token.trim();

        if (trimmed.startsWith(SCHEME)) {
            return trimmed;
        }

        return SCHEME + " " + trimmed;
    }

    /*
     * Plain text, not HTML. The one thing this message has to do is
     * carry six digits into somebody's hand, and plain text arrives
     * intact in every client, never lands in a promotions tab for its
     * markup, and cannot be mistaken for a phishing template.
     */
    private String body(String fullName, String code) {

        String greeting = fullName == null || fullName.isBlank()
                ? "Hello,"
                : "Hello " + fullName + ",";

        return greeting + "\n\n"
                + "Your Gate Pass password reset code is:\n\n"
                + "    " + code + "\n\n"
                + "Type it into the app to set a new password. The code "
                + "expires in " + validityMinutes + " minutes and can be "
                + "used once.\n\n"
                + "If you did not ask to reset your password, you can "
                + "ignore this message - nothing has changed, and whoever "
                + "asked cannot get in without this code.\n\n"
                + "Gate Pass";
    }
}
