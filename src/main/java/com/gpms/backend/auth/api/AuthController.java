package com.gpms.backend.auth.api;

import com.gpms.backend.auth.application.AuthService;
import com.gpms.backend.auth.domain.ChangePasswordRequest;
import com.gpms.backend.auth.domain.LoginRequest;
import com.gpms.backend.auth.domain.LoginResponse;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    @GetMapping("/me")
    public LoginResponse me() {
        return authService.me();
    }
}
