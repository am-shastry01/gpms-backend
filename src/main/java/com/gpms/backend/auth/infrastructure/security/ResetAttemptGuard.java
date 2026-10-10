package com.gpms.backend.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Slows down guessing at the forgotten-password form.
 *
 * The form takes an email address and a mobile number, and a mobile
 * number is ten digits - so without a limit it is something you can
 * simply try until it works. Five wrong answers from one address buys
 * a thirty minute wait; a correct one clears the count.
 *
 * In memory, per instance, on purpose: it is a speed bump, not an
 * access-control list, and a restart clearing it is an acceptable
 * price for not putting a failed-attempt table in the database. Keyed
 * on the remote address, with X-Forwarded-For honoured only for its
 * first hop so a client cannot spoof itself a clean slate.
 */
@Component
public class ResetAttemptGuard {

    private static final int MAX_FAILURES = 5;

    private static final Duration BLOCK_FOR = Duration.ofMinutes(30);

    private record Attempts(AtomicInteger count, Instant blockedUntil) {
    }

    private final Map<String, Attempts> byAddress = new ConcurrentHashMap<>();

    public boolean isBlocked(HttpServletRequest request) {
        Attempts attempts = byAddress.get(key(request));

        if (attempts == null || attempts.blockedUntil() == null) {
            return false;
        }

        if (Instant.now().isAfter(attempts.blockedUntil())) {
            byAddress.remove(key(request));
            return false;
        }

        return true;
    }

    public void recordFailure(HttpServletRequest request) {
        String key = key(request);

        Attempts attempts = byAddress.computeIfAbsent(
                key, unused -> new Attempts(new AtomicInteger(), null));

        int failures = attempts.count().incrementAndGet();

        if (failures >= MAX_FAILURES) {
            byAddress.put(key, new Attempts(
                    attempts.count(), Instant.now().plus(BLOCK_FOR)));
        }
    }

    public void recordSuccess(HttpServletRequest request) {
        byAddress.remove(key(request));
    }

    public long minutesRemaining(HttpServletRequest request) {
        Attempts attempts = byAddress.get(key(request));

        if (attempts == null || attempts.blockedUntil() == null) {
            return 0L;
        }

        long minutes = Duration.between(Instant.now(), attempts.blockedUntil())
                .toMinutes();

        return Math.max(minutes, 1L);
    }

    private String key(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
