package com.gpms.backend.auth.infrastructure;

import com.gpms.backend.auth.domain.PasswordResetOtp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtp, UUID> {

    /**
     * The newest code issued to an address that has not been used yet.
     *
     * Newest rather than "the" code: a person who presses resend has
     * two live rows for a moment, and the one they are reading off
     * their screen is the last one sent.
     */
    Optional<PasswordResetOtp>
            findFirstByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(String email);

    /**
     * Throws away every code for one address.
     *
     * Called before a new one is issued, so only the latest code can
     * ever work, and again after a successful reset.
     */
    @Modifying
    @Query("delete from PasswordResetOtp o where o.email = :email")
    void deleteAllByEmail(@Param("email") String email);

    /** Housekeeping: codes that are long past being any use. */
    @Modifying
    @Query("delete from PasswordResetOtp o where o.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
