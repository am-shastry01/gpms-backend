package com.gpms.backend.auth.application;

import com.gpms.backend.auth.domain.LoginRequest;
import com.gpms.backend.auth.domain.LoginResponse;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.gpms.backend.auth.domain.ChangePasswordRequest;
import com.gpms.backend.common.exception.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final com.gpms.backend.common.service.CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            com.gpms.backend.common.service.CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        /*
         * Looked up the same way the authentication did, by username
         * or email - otherwise signing in with an email address would
         * authenticate and then fail to find the account.
         */
        User user = userRepository.findByUsernameOrEmail(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse me() {
        return buildResponse(currentUserService.requireCurrentUser());
    }

    /*
     * Replaces the caller's own password.
     *
     * This is also how a temporary password becomes a real one: the
     * flag comes down here and nowhere else, so an account cannot
     * leave the "create a new password" screen without a password its
     * owner chose. A fresh token comes back, because the one the
     * phone holds was minted while the flag was still up.
     */
    @Transactional
    public LoginResponse changePassword(ChangePasswordRequest request) {

        User user = currentUserService.requireCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedOperationException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new UnauthorizedOperationException(
                    "The new password has to be different from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        /*
         * Stamped before the new token is minted, so the token returned
         * from here survives and every older one stops working.
         */
        user.setPasswordChangedAt(java.time.Instant.now());

        return buildResponse(userRepository.save(user));
    }

    private LoginResponse buildResponse(User user) {
        return new LoginResponse(
                jwtService.generateToken(user),
                new LoginResponse.UserContext(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail(),
                        /*
                         * A user is not guaranteed to have a warehouse -
                         * a global admin typically has none. Reading
                         * through a null warehouse here made login fail
                         * with a 500 for those accounts.
                         */
                        user.getWarehouse() == null ? null : user.getWarehouse().getId(),
                        user.getWarehouse() == null ? null : user.getWarehouse().getName(),
                        user.getRoles().stream().map(role -> role.getCode()).sorted().toList(),
                        user.isMustChangePassword()
                )
        );
    }
}
