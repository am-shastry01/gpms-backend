package com.gpms.backend.gatepass.application;

import com.gpms.backend.config.SmsProperties;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * Issues and checks the token behind a driver's pass link.
 *
 * The link is addressed by this token rather than the request's UUID
 * so that it can expire, be revoked, and be consumed exactly once -
 * none of which is possible with a primary key.
 */
@Service
public class GatePassTokenService {

    /* 32 bytes -> 43 url-safe characters, well inside VARCHAR(64). */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SmsProperties smsProperties;

    public GatePassTokenService(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    /**
     * Stamps a fresh token onto the request. Re-approving a pass
     * issues a new one, which invalidates any link already sent.
     */
    public String issue(GatePassRequest gatePassRequest) {

        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        Instant now = Instant.now();

        gatePassRequest.setAccessToken(token);
        gatePassRequest.setTokenIssuedAt(now);
        gatePassRequest.setTokenExpiresAt(
                now.plus(smsProperties.getLinkTtlHours(), ChronoUnit.HOURS)
        );
        gatePassRequest.setTokenConsumedAt(null);

        return token;
    }

    public boolean isExpired(GatePassRequest gatePassRequest) {

        Instant expiresAt = gatePassRequest.getTokenExpiresAt();

        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isConsumed(GatePassRequest gatePassRequest) {
        return gatePassRequest.getTokenConsumedAt() != null;
    }

    public void markConsumed(GatePassRequest gatePassRequest) {
        gatePassRequest.setTokenConsumedAt(Instant.now());
    }

    /** The URL texted to the driver. */
    public String buildDriverLink(String publicBaseUrl, String token) {

        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + "/public/gate-pass/" + token;
    }
}
