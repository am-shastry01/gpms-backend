package com.gpms.backend.auth.application;

import com.gpms.backend.auth.domain.LoginRequest;
import com.gpms.backend.auth.domain.LoginResponse;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final com.gpms.backend.common.service.CurrentUserService currentUserService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            com.gpms.backend.common.service.CurrentUserService currentUserService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        User user = userRepository.findByUsernameIgnoreCaseAndDeletedFalse(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return buildResponse(user);
    }

    public LoginResponse me() {
        return buildResponse(currentUserService.requireCurrentUser());
    }

    private LoginResponse buildResponse(User user) {
        return new LoginResponse(
                jwtService.generateToken(user),
                new LoginResponse.UserContext(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getWarehouse().getId(),
                        user.getWarehouse().getName(),
                        user.getRoles().stream().map(role -> role.getCode()).sorted().toList()
                )
        );
    }
}
