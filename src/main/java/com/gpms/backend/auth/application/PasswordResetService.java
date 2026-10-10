package com.gpms.backend.auth.application;

import com.gpms.backend.auth.domain.ForgotPasswordRequest;
import com.gpms.backend.auth.domain.PasswordResetOtp;
import com.gpms.backend.auth.domain.SendOtpRequest;
import com.gpms.backend.auth.infrastructure.PasswordResetOtpRepository;
import com.gpms.backend.common.exception.UnauthorizedOperationException;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resetting a password you cannot remember, by a code sent to the
 * address you sign in with.
 *
 * Two steps, two endpoints. The first sends a code; the second accepts
 * the code and the new password together, so there is no window in
 * between where a verified-but-unused code sits around waiting to be
 * replayed.
 *
 * The rules the whole thing rests on:
 *
 *   - Sending NEVER says whether the address has an account. The reply
 *     is identical either way, so this form cannot be used to find out
 *     who works here.
 *   - Only the newest code works. Issuing one throws away the
 *     previous ones, so pressing resend does not leave a trail of
 *     usable codes behind.
 *   - A code is held as a hash, dies after a few wrong guesses, and is
 *     deleted the moment it is spent.
 *   - A successful reset stamps the password-change time, which
 *     invalidates every token issued before now - including one held by
 *     whoever had the phone.
 */
@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Wrong guesses allowed on one code before it is thrown away. */
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;

    private final PasswordResetOtpRepository otpRepository;

    private final PasswordResetMailer mailer;

    private final PasswordEncoder passwordEncoder;

    private final int validityMinutes;

    private final int resendSeconds;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetOtpRepository otpRepository,
            PasswordResetMailer mailer,
            PasswordEncoder passwordEncoder,
            @Value("${app.password-reset.validity-minutes:10}") int validityMinutes,
            @Value("${app.password-reset.resend-seconds:60}") int resendSeconds
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.mailer = mailer;
        this.passwordEncoder = passwordEncoder;
        this.validityMinutes = validityMinutes;
        this.resendSeconds = resendSeconds;
    }

    // =============================================================
    // STEP 1 - SEND
    // =============================================================

    @Transactional
    public void sendCode(SendOtpRequest request) {

        String email = normalise(request.email());

        Optional<User> account = userRepository
                .findByEmailIgnoreCaseAndDeletedFalse(email)
                .filter(User::isActive)
                .filter(candidate -> candidate.getCompany() != null
                        && candidate.getCompany().isActive()
                        && !candidate.getCompany().isDeleted());

        /*
         * An address with no account gets no code and no complaint.
         * Returning here - rather than throwing - is what keeps the
         * reply identical for an address that exists and one that does
         * not.
         */
        if (account.isEmpty()) {
            return;
        }

        /*
         * A resend cooldown, so the form cannot be used to post mail
         * at somebody. The caller is not told it was throttled, for
         * the same reason as above.
         */
        Optional<PasswordResetOtp> existing = otpRepository
                .findFirstByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(email);

        if (existing.isPresent() && existing.get().getCreatedAt() != null) {

            Instant readyAt = existing.get().getCreatedAt()
                    .plusSeconds(resendSeconds);

            if (Instant.now().isBefore(readyAt)) {
                return;
            }
        }

        /*
         * Previous codes go first. Leaving them alive would mean every
         * resend added another working code to the pile.
         */
        otpRepository.deleteAllByEmail(email);

        String code = sixDigits();

        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(email);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(Instant.now().plus(Duration.ofMinutes(validityMinutes)));
        otp.setAttempts(0);
        otp.setCreatedAt(Instant.now());

        otpRepository.save(otp);

        mailer.sendCode(email, account.get().getFullName(), code);
    }

    // =============================================================
    // STEP 2 - VERIFY AND SET
    // =============================================================

    @Transactional
    public void resetWithCode(ForgotPasswordRequest request) {

        String email = normalise(request.email());

        PasswordResetOtp otp = otpRepository
                .findFirstByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(PasswordResetService::wrongCode);

        if (Instant.now().isAfter(otp.getExpiresAt())) {
            otpRepository.deleteAllByEmail(email);
            throw new UnauthorizedOperationException(
                    "That code has expired. Ask for a new one.");
        }

        if (!passwordEncoder.matches(request.code().trim(), otp.getCodeHash())) {

            otp.setAttempts(otp.getAttempts() + 1);

            if (otp.getAttempts() >= MAX_ATTEMPTS) {
                /*
                 * Too many guesses and the code is gone, not merely
                 * locked - a six digit number that survives unlimited
                 * guessing is not a secret.
                 */
                otpRepository.deleteAllByEmail(email);
                throw new UnauthorizedOperationException(
                        "Too many wrong codes. Ask for a new one.");
            }

            otpRepository.save(otp);
            throw wrongCode();
        }

        /*
         * The code was right, so the account must still be reachable.
         * It is re-checked rather than trusted from step one: an
         * account can be suspended in the ten minutes between the two
         * calls, and this is the call that changes a password.
         */
        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedFalse(email)
                .filter(User::isActive)
                .filter(candidate -> candidate.getCompany() != null
                        && candidate.getCompany().isActive()
                        && !candidate.getCompany().isDeleted())
                .orElseThrow(() -> new UnauthorizedOperationException(
                        "That account can no longer sign in. "
                                + "Ask your admin to check it."));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        /*
         * Stamped last: every token minted before this instant stops
         * working, which is the point of resetting a password you think
         * somebody else may know.
         */
        user.setPasswordChangedAt(Instant.now());

        userRepository.save(user);

        /*
         * The code is spent. Deleted rather than flagged consumed -
         * there is nothing useful left in the row, and a reset code is
         * not something to keep a history of.
         */
        otpRepository.deleteAllByEmail(email);
    }

    // =============================================================
    // BUILDING BLOCKS
    // =============================================================

    /*
     * The same message for "no code was ever sent to that address" and
     * "that is the wrong code". Telling the two apart would turn the
     * verify step into the account check that the send step carefully
     * refuses to be.
     */
    private static UnauthorizedOperationException wrongCode() {
        return new UnauthorizedOperationException(
                "That code is not right. Check it and try again.");
    }

    /*
     * SecureRandom, and the full six digit range including codes that
     * start with a zero - formatting rather than arithmetic keeps the
     * leading zero, and a generator that quietly skipped them would
     * throw away a tenth of the keyspace.
     */
    private String sixDigits() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
