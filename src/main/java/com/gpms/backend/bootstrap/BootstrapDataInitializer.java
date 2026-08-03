package com.gpms.backend.bootstrap;

import com.gpms.backend.config.BootstrapProperties;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.SystemRole;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.RoleRepository;
import com.gpms.backend.user.infrastructure.UserRepository;
import com.gpms.backend.warehouse.domain.Warehouse;
import com.gpms.backend.warehouse.infrastructure.WarehouseRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapDataInitializer implements CommandLineRunner {

    private final BootstrapProperties bootstrapProperties;
    private final RoleRepository roleRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapDataInitializer(
            BootstrapProperties bootstrapProperties,
            RoleRepository roleRepository,
            WarehouseRepository warehouseRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.bootstrapProperties = bootstrapProperties;
        this.roleRepository = roleRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!bootstrapProperties.isEnabled()) {
            return;
        }
        ensureRoles();
        Warehouse warehouse = warehouseRepository.findByCodeIgnoreCaseAndDeletedFalse(bootstrapProperties.getWarehouseCode())
                .orElseGet(this::createWarehouse);
        ensureUser(
                bootstrapProperties.getAdminUsername(),
                bootstrapProperties.getAdminPassword(),
                bootstrapProperties.getAdminEmail(),
                bootstrapProperties.getAdminFullName(),
                warehouse,
                List.of(SystemRole.ADMIN.name())
        );
        ensureUser(
                bootstrapProperties.getManagerUsername(),
                bootstrapProperties.getManagerPassword(),
                bootstrapProperties.getManagerEmail(),
                bootstrapProperties.getManagerFullName(),
                warehouse,
                List.of(SystemRole.MANAGER.name())
        );
        ensureUser(
                bootstrapProperties.getEmployeeUsername(),
                bootstrapProperties.getEmployeePassword(),
                bootstrapProperties.getEmployeeEmail(),
                bootstrapProperties.getEmployeeFullName(),
                warehouse,
                List.of(SystemRole.EMPLOYEE.name())
        );
        ensureUser(
                bootstrapProperties.getSecurityUsername(),
                bootstrapProperties.getSecurityPassword(),
                bootstrapProperties.getSecurityEmail(),
                bootstrapProperties.getSecurityFullName(),
                warehouse,
                List.of(SystemRole.SECURITY.name())
        );
    }

    private void ensureRoles() {
        for (SystemRole role : SystemRole.values()) {
            roleRepository.findByCodeIgnoreCase(role.name()).orElseGet(() -> {
                Role entity = new Role();
                entity.setCode(role.name());
                entity.setDescription(role.name() + " role");
                return roleRepository.save(entity);
            });
        }
    }

    private Warehouse createWarehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(bootstrapProperties.getWarehouseCode().trim().toUpperCase());
        warehouse.setName(bootstrapProperties.getWarehouseName().trim());
        warehouse.setTimezone("Asia/Kolkata");
        warehouse.setActive(true);
        return warehouseRepository.save(warehouse);
    }

    private void ensureUser(
            String username,
            String password,
            String email,
            String fullName,
            Warehouse warehouse,
            List<String> roleCodes
    ) {
        if (userRepository.findByUsernameIgnoreCaseAndDeletedFalse(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setEmail(email.toLowerCase());
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setWarehouse(warehouse);
        user.setActive(true);
        user.setRoles(roleCodes.stream()
                .map(code -> roleRepository.findByCodeIgnoreCase(code).orElseThrow())
                .collect(java.util.stream.Collectors.toSet()));
        userRepository.save(user);
    }
}
