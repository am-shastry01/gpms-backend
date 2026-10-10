package com.gpms.backend.auth.api;

import com.gpms.backend.auth.application.AuthService;
import com.gpms.backend.auth.domain.ChangePasswordRequest;
import com.gpms.backend.auth.domain.ForgotPasswordRequest;
import com.gpms.backend.auth.domain.LoginRequest;
import com.gpms.backend.auth.domain.LoginResponse;
import com.gpms.backend.auth.domain.MessageResponse;
import com.gpms.backend.auth.domain.SendOtpRequest;
import com.gpms.backend.auth.application.PasswordResetService;
import com.gpms.backend.auth.infrastructure.security.ResetAttemptGuard;
import com.gpms.backend.common.exception.UnauthorizedOperationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    private final ResetAttemptGuard resetAttemptGuard;

    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            ResetAttemptGuard resetAttemptGuard,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.resetAttemptGuard = resetAttemptGuard;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /*
     * Everything under /auth is permitAll so that login itself can be
     * reached, so this one says out loud that it needs a token.
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public LoginResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

    /*
     * Step one of a forgotten password: email a six digit code.
     *
     * Open on purpose - somebody who has forgotten their password has
     * no token to send.
     *
     * The reply is the same whether or not that address has an account,
     * and whether or not the mail actually went out. Anything else turns
     * this endpoint into a way to find out who works here. The throttle
     * is what stands in for a token: without it the same reply could be
     * farmed one address at a time.
     */
    @PostMapping("/forgot-password/send-otp")
    public MessageResponse sendResetCode(
            @Valid @RequestBody SendOtpRequest request,
            HttpServletRequest httpRequest
    ) {

        if (resetAttemptGuard.isBlocked(httpRequest)) {
            throw new UnauthorizedOperationException(
                    "Too many attempts. Try again in "
                            + resetAttemptGuard.minutesRemaining(httpRequest)
                            + " minutes.");
        }

        passwordResetService.sendCode(request);

        return new MessageResponse(
                "If that email address has an account, a 6 digit code is "
                        + "on its way to it.");
    }

    /*
     * Step two: the code and the new password together.
     *
     * A wrong code counts against the throttle as well as against the
     * code's own attempt limit - the code dies after a handful of
     * guesses, and the address behind it stops being able to guess at
     * all after a handful of codes.
     */
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {

        if (resetAttemptGuard.isBlocked(httpRequest)) {
            throw new UnauthorizedOperationException(
                    "Too many attempts. Try again in "
                            + resetAttemptGuard.minutesRemaining(httpRequest)
                            + " minutes.");
        }

        try {
            passwordResetService.resetWithCode(request);
        } catch (UnauthorizedOperationException failure) {
            resetAttemptGuard.recordFailure(httpRequest);
            throw failure;
        }

        resetAttemptGuard.recordSuccess(httpRequest);

        return new MessageResponse(
                "Your password has been changed. Sign in with the new one.");
    }

    @GetMapping("/me")
    public LoginResponse me() {
        return authService.me();
    }
}
