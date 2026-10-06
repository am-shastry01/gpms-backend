package com.gpms.backend.user.api;

import com.gpms.backend.user.api.dto.ResetPasswordRequest;
import com.gpms.backend.user.api.dto.UserCreateRequest;
import com.gpms.backend.user.api.dto.UserDeleteResponse;
import com.gpms.backend.user.api.dto.UserResponse;
import com.gpms.backend.user.application.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PostMapping("/{id}/reset-password")
    public UserResponse resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return userService.resetPassword(id, request);
    }

    @DeleteMapping("/{id}")
    public UserDeleteResponse delete(@PathVariable UUID id) {
        return userService.delete(id);
    }
}
