package com.gpms.backend.auth.application;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Sends the forgotten-password code.
 *
 * SMTP through the company's own Zoho mailbox, because that mailbox
 * already exists and a transactional-email provider would be one more
 * account to run for one message a week.
 *
 * A send that fails is logged and swallowed. The caller must not be
 * told whether the address exists - and it also must not be told
 * whether OUR mail server is having a bad afternoon, because both
 * answers come back through the same reply and one of them would leak
 * the other. The person sees the same "if that address has an account,
 * a code is on its way" either way, and an operator reading the log
 * sees exactly what went wrong.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log =
            LoggerFactory.getLogger(PasswordResetMailer.class);

    private final JavaMailSender mailSender;

    private final String from;

    private final String fromName;

    private final int validityMinutes;

    public PasswordResetMailer(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String from,
            @Value("${app.mail.from-name:Gate Pass}") String fromName,
            @Value("${app.password-reset.validity-minutes:10}") int validityMinutes
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
        this.validityMinutes = validityMinutes;
    }

    public void sendCode(String email, String fullName, String code) {

        if (from == null || from.isBlank()) {
            /*
             * No mailbox configured. On a developer machine that is
             * normal, and the code is printed instead so the flow can
             * be walked through end to end without a mail server.
             */
            log.warn("Mail is not configured; password reset code for {} is {}",
                    email, code);
            return;
        }

        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(from, fromName);
            helper.setTo(email);
            helper.setSubject("Your Gate Pass password reset code");
            helper.setText(body(fullName, code), false);

            mailSender.send(message);

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
