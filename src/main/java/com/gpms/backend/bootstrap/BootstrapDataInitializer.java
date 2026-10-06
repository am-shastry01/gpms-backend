package com.gpms.backend.bootstrap;

import com.gpms.backend.company.domain.Company;
import com.gpms.backend.company.infrastructure.CompanyRepository;
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
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapDataInitializer(
            BootstrapProperties bootstrapProperties,
            RoleRepository roleRepository,
            WarehouseRepository warehouseRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.bootstrapProperties = bootstrapProperties;
        this.roleRepository = roleRepository;
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!bootstrapProperties.isEnabled()) {
            return;
        }
        /*
         * Roles always. They are referenced by code from every service,
         * so a database without them is not a database this application
         * can run against.
         */
        ensureRoles();

        /*
         * Everything below seeds an EMPTY database, and only an empty
         * one. Once a company exists, companies are the super-admin
         * site's business and the bootstrap keeps its hands off.
         *
         * It used to take "the first company ordered by NAME" and add
         * its warehouse and three default accounts to it on every
         * startup - so onboarding a client called Acme handed them a
         * stray Primary Warehouse and an admin, a manager and an
         * employee they never asked for, because A sorts before Y.
         */
        if (!companyRepository.findAllByDeletedFalseOrderByNameAsc().isEmpty()) {
            return;
        }

        Company company = createCompany();

        Warehouse warehouse = warehouseRepository
                .findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(
                        bootstrapProperties.getWarehouseCode(), company.getId())
                .orElseGet(() -> createWarehouse(company));
        ensureUser(
                bootstrapProperties.getAdminUsername(),
                bootstrapProperties.getAdminPassword(),
                bootstrapProperties.getAdminEmail(),
                bootstrapProperties.getAdminFullName(),
                company,
                warehouse,
                List.of(SystemRole.ADMIN.name())
        );
        ensureUser(
                bootstrapProperties.getManagerUsername(),
                bootstrapProperties.getManagerPassword(),
                bootstrapProperties.getManagerEmail(),
                bootstrapProperties.getManagerFullName(),
                company,
                warehouse,
                List.of(SystemRole.MANAGER.name())
        );
        ensureUser(
                bootstrapProperties.getEmployeeUsername(),
                bootstrapProperties.getEmployeePassword(),
                bootstrapProperties.getEmployeeEmail(),
                bootstrapProperties.getEmployeeFullName(),
                company,
                warehouse,
                List.of(SystemRole.EMPLOYEE.name())
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

    private Company createCompany() {
        Company company = new Company();
        company.setName("Company 1");
        company.setActive(true);
        return companyRepository.save(company);
    }

    private Warehouse createWarehouse(Company company) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCompany(company);
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
            Company company,
            Warehouse warehouse,
            List<String> roleCodes
    ) {
        if (userRepository.findByUsernameIgnoreCaseAndDeletedFalse(username).isPresent()) {
            return;
        }

        /*
         * The username column is unique across deleted rows too, so a
         * seed account that was removed during testing cannot simply be
         * inserted again - the insert would fail and take startup with
         * it. Revive that row instead: same account, password reset to
         * the seed one, and must-change raised so it still has to be
         * claimed by whoever signs in.
         */
        User revived = userRepository.findByUsernameOrEmailIncludingDeleted(username)
                .stream()
                .findFirst()
                .orElse(null);

        if (revived != null) {
            revived.setDeleted(false);
            revived.setDeletedAt(null);
            revived.setActive(true);
            revived.setPasswordHash(passwordEncoder.encode(password));
            revived.setPasswordChangedAt(java.time.Instant.now());
            revived.setMustChangePassword(true);
            userRepository.save(revived);
            return;
        }

        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setEmail(email.toLowerCase());
        user.setFullName(fullName);
        user.setCompany(company);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordChangedAt(java.time.Instant.now());
        user.setWarehouse(warehouse);
        user.setActive(true);
        /*
         * The seed passwords are published in application.yml, so an
         * account created here is handed over, not owned: it cannot be
         * used for anything until the person signing in replaces the
         * password. That closes the known-default hole without stopping
         * a fresh database from being usable.
         */
        user.setMustChangePassword(true);
        user.setRoles(roleCodes.stream()
                .map(code -> roleRepository.findByCodeIgnoreCase(code).orElseThrow())
                .collect(java.util.stream.Collectors.toSet()));
        userRepository.save(user);
    }
}
