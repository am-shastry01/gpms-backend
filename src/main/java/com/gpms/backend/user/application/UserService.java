package com.gpms.backend.user.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.user.api.dto.UserCreateRequest;
import com.gpms.backend.user.api.dto.UserResponse;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.RoleRepository;
import com.gpms.backend.user.infrastructure.UserRepository;
import com.gpms.backend.warehouse.domain.Warehouse;
import com.gpms.backend.warehouse.infrastructure.WarehouseRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            WarehouseRepository warehouseRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.warehouseRepository = warehouseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAll() {
        return userRepository.findAllByDeletedFalse().stream().map(this::toResponse).toList();
    }

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already exists");
        }
        Warehouse warehouse = warehouseRepository.findByIdAndDeletedFalse(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        User user = new User();
        user.setUsername(request.username().trim().toLowerCase());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setMobileNumber(request.mobileNumber());
        user.setWarehouse(warehouse);
        user.setActive(request.active() == null || request.active());
        user.setRoles(resolveRoles(request.roles()));
        return toResponse(userRepository.save(user));
    }

    public void delete(UUID id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    private Set<Role> resolveRoles(List<String> roleCodes) {
        return roleCodes.stream()
                .map(code -> roleRepository.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + code)))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getMobileNumber(),
                user.isActive(),
                user.getWarehouse().getId(),
                user.getWarehouse().getName(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
