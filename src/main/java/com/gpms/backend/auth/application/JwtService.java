package com.gpms.backend.auth.application;

import com.gpms.backend.config.JwtProperties;
import com.gpms.backend.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    /*
     * Read rather than injected as a Profiles object so the check below
     * works the same whether the app was started by Spring Boot, a
     * test, or docker compose.
     */
    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:}")
    private String activeProfiles;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(role -> role.getCode()).toList();
        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getAccessTokenMinutes(), ChronoUnit.MINUTES)))
                .signWith(getSigningKey());
        if (user.getId() != null) {
            builder.claim("userId", user.getId().toString());
        }
        return builder.compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Valid for this user, and not minted before their current password.
     *
     * There is no revocation list, so without the second half of that
     * sentence changing a password left every token already issued
     * working until it expired - hours on a phone somebody else now
     * has. A second of tolerance absorbs the clock skew between the
     * row's timestamp and the token's.
     */
    public boolean isTokenValid(String token, String username, Instant passwordChangedAt) {

        if (!isTokenValid(token, username)) {
            return false;
        }

        if (passwordChangedAt == null) {
            return true;
        }

        Date issuedAt = parseClaims(token).getIssuedAt();

        return issuedAt != null
                && !issuedAt.toInstant().isBefore(passwordChangedAt.minusSeconds(1));
    }

    public boolean isTokenValid(String token, String username) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        Date expiration = claims.getExpiration();
        return subject != null
                && subject.equalsIgnoreCase(username)
                && expiration != null
                && expiration.after(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /*
     * The signing key IS the authentication system.
     *
     * The placeholder below ships in this repository and in
     * docker-compose.yml, so anybody holding the source could mint a
     * token claiming ROLE_ADMIN against a deployment that forgot to set
     * APP_JWT_SECRET. Refusing to start is the only safe answer; the
     * local profile is let through so a dev machine still runs.
     */
    private static final String PUBLISHED_PLACEHOLDER =
            "change-me-to-a-32-character-minimum-secret";

    /*
     * A development machine opts in explicitly with
     * APP_ALLOW_INSECURE_JWT_SECRET=true (docker-compose.yml sets it).
     * Anywhere that flag is absent, the placeholder stops the server
     * from starting rather than quietly accepting forged admin tokens.
     */
    @org.springframework.beans.factory.annotation.Value(
            "${app.jwt.allow-insecure-secret:false}")
    private boolean allowInsecureSecret;

    @jakarta.annotation.PostConstruct
    void refuseAnInsecureSecret() {

        String secret = jwtProperties.getSecret() == null ? "" : jwtProperties.getSecret();

        boolean waved = allowInsecureSecret
                || (activeProfiles != null && activeProfiles.contains("local"));

        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be at least 32 characters.");
        }

        if (!PUBLISHED_PLACEHOLDER.equals(secret)) {
            return;
        }

        if (waved) {
            org.slf4j.LoggerFactory.getLogger(JwtService.class).warn(
                    "Running on the placeholder JWT secret that ships in this "
                            + "repository. Anyone with the source can forge an "
                            + "admin token - set APP_JWT_SECRET before this "
                            + "reaches a real server.");
            return;
        }

        throw new IllegalStateException(
                "APP_JWT_SECRET is still the placeholder from the repository. "
                        + "Set a private 32+ character secret, or set "
                        + "APP_ALLOW_INSECURE_JWT_SECRET=true if this really is "
                        + "a development machine.");
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().length() >= 32
                ? jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
