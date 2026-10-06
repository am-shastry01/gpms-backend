package com.gpms.backend.superadmin.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.company.domain.Company;
import com.gpms.backend.company.infrastructure.CompanyRepository;
import com.gpms.backend.gatepass.infrastructure.GatePassRequestRepository;
import com.gpms.backend.superadmin.api.CompanyOnboardingForm;
import com.gpms.backend.user.domain.Role;
import com.gpms.backend.user.domain.SystemRole;
import com.gpms.backend.user.domain.User;
import com.gpms.backend.user.infrastructure.RoleRepository;
import com.gpms.backend.user.infrastructure.UserRepository;
import com.gpms.backend.warehouse.domain.Warehouse;
import com.gpms.backend.warehouse.infrastructure.WarehouseRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Onboarding and oversight, from outside any company.
 *
 * Every other service in this codebase is fenced to the caller's own
 * company. This one deliberately is not - it is the only place that
 * reads across clients, and it is reachable only from the /superadmin
 * filter chain, whose credentials do not exist in the users table.
 */
@Service
@Transactional
public class SuperAdminService {

    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GatePassRequestRepository gatePassRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminService(
            CompanyRepository companyRepository,
            WarehouseRepository warehouseRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            GatePassRequestRepository gatePassRequestRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.gatePassRequestRepository = gatePassRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------------
    // LIST
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SuperAdminView.CompanyRow> companies() {

        List<SuperAdminView.CompanyRow> rows = new ArrayList<>();

        for (Company company : companyRepository.findAllByDeletedFalseOrderByNameAsc()) {

            List<Warehouse> warehouses = warehouseRepository
                    .findAllByCompanyIdAndDeletedFalseOrderByNameAsc(company.getId());

            long passes = gatePassRequestRepository
                    .countByWarehouseForCompany(company.getId())
                    .stream()
                    .mapToLong(row -> ((Number) row[1]).longValue())
                    .sum();

            rows.add(new SuperAdminView.CompanyRow(
                    company.getId(),
                    company.getName(),
                    company.getCode(),
                    company.getEmail(),
                    company.getPhoneNumber(),
                    company.getGstNumber(),
                    company.isActive(),
                    warehouses.size(),
                    userRepository.findAllByCompanyIdAndDeletedFalse(company.getId()).size(),
                    passes,
                    company.getCreatedAt()
            ));
        }

        return rows;
    }

    // ---------------------------------------------------------------
    // DETAIL
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public SuperAdminView.CompanyDetail company(UUID id) {

        Company company = companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        /*
         * One grouped query for the pass counts rather than one per
         * warehouse - a company with a dozen sites would otherwise mean
         * a dozen round trips to render one page.
         */
        Map<UUID, Long> passesByWarehouse = new HashMap<>();

        for (Object[] row : gatePassRequestRepository.countByWarehouseForCompany(id)) {
            passesByWarehouse.put((UUID) row[0], ((Number) row[1]).longValue());
        }

        List<User> people = userRepository.findAllByCompanyIdAndDeletedFalse(id);

        List<SuperAdminView.WarehouseRow> warehouseRows = new ArrayList<>();

        for (Warehouse warehouse : warehouseRepository
                .findAllByCompanyIdAndDeletedFalseOrderByNameAsc(id)) {

            long managers = people.stream()
                    .filter(person -> atWarehouse(person, warehouse.getId()))
                    .filter(person -> roleOf(person).equals("MANAGER"))
                    .count();

            long employees = people.stream()
                    .filter(person -> atWarehouse(person, warehouse.getId()))
                    .filter(person -> roleOf(person).equals("EMPLOYEE"))
                    .count();

            warehouseRows.add(new SuperAdminView.WarehouseRow(
                    warehouse.getId(),
                    warehouse.getCode(),
                    warehouse.getName(),
                    warehouse.getCity(),
                    warehouse.isActive(),
                    managers,
                    employees,
                    passesByWarehouse.getOrDefault(warehouse.getId(), 0L)
            ));
        }

        List<SuperAdminView.PersonRow> personRows = people.stream()
                .sorted((left, right) -> left.getFullName()
                        .compareToIgnoreCase(right.getFullName()))
                .map(person -> new SuperAdminView.PersonRow(
                        person.getId(),
                        person.getFullName(),
                        person.getEmail(),
                        person.getMobileNumber(),
                        roleOf(person),
                        person.getWarehouse() == null
                                ? "All warehouses"
                                : person.getWarehouse().getName(),
                        person.isActive(),
                        person.isMustChangePassword()
                ))
                .toList();

        return new SuperAdminView.CompanyDetail(
                company.getId(),
                company.getName(),
                company.getCode(),
                company.getGstNumber(),
                company.getPhoneNumber(),
                company.getEmail(),
                company.getWebsite(),
                company.addressOneLine(),
                company.isActive(),
                company.getCreatedAt(),
                passesByWarehouse.values().stream().mapToLong(Long::longValue).sum(),
                warehouseRows,
                personRows
        );
    }

    // ---------------------------------------------------------------
    // ONBOARDING
    // ---------------------------------------------------------------

    /**
     * Creates the company, its warehouses and its first admin in one
     * transaction - so a half-made client, with a company nobody can
     * sign in to, cannot be left behind by a failure half way down.
     *
     * Returns the company id so the caller can go straight to its page.
     */
    public UUID onboard(CompanyOnboardingForm form) {

        List<String[]> warehouseRows = form.warehouseRows();

        if (warehouseRows.isEmpty()) {
            throw new ConflictException("At least one warehouse is required");
        }

        for (String[] row : warehouseRows) {
            if (row[0].isEmpty()) {
                throw new ConflictException(
                        "A warehouse number was given without a name");
            }
        }

        /*
         * Numbers must be distinct inside the company - the database
         * index says so too, but catching it here keeps the typed form
         * intact instead of losing it to a constraint violation.
         */
        Set<String> seen = new java.util.HashSet<>();

        for (String[] row : warehouseRows) {
            String number = row[1].isEmpty() ? "" : row[1].toUpperCase();
            if (!number.isEmpty() && !seen.add(number)) {
                throw new ConflictException(
                        "Two warehouses were given the number " + row[1]);
            }
        }

        String companyCode = form.getCode().trim().toUpperCase();

        if (!companyCode.matches("[A-Z0-9]{2,12}")) {
            throw new ConflictException(
                    "The company code must be 2 to 12 letters or digits");
        }

        if (companyRepository.existsByCodeIgnoreCaseAndDeletedFalse(companyCode)) {
            throw new ConflictException(
                    "The code " + companyCode + " is already taken by another company");
        }

        String adminEmail = form.getAdminEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(adminEmail)
                || userRepository.existsByUsernameIgnoreCase(adminEmail)) {
            throw new ConflictException(
                    "Somebody already signs in with " + adminEmail);
        }

        if (companyRepository.existsByNameIgnoreCaseAndDeletedFalse(form.getName().trim())) {
            throw new ConflictException(
                    "A company called " + form.getName().trim() + " already exists");
        }

        Company company = new Company();
        company.setName(form.getName().trim());
        company.setCode(companyCode);
        company.setGstNumber(blankToNull(form.getGstNumber()));
        company.setPhoneNumber(blankToNull(form.getPhoneNumber()));
        company.setEmail(blankToNull(form.getEmail()));
        company.setWebsite(blankToNull(form.getWebsite()));
        company.setAddressLine1(blankToNull(form.getAddressLine1()));
        company.setAddressLine2(blankToNull(form.getAddressLine2()));
        company.setCity(blankToNull(form.getCity()));
        company.setState(blankToNull(form.getState()));
        company.setPostalCode(blankToNull(form.getPostalCode()));
        company.setActive(true);

        Company saved = companyRepository.save(company);

        int index = 1;

        for (String[] row : warehouseRows) {
            Warehouse warehouse = new Warehouse();
            warehouse.setCompany(saved);
            /*
             * The number as typed - "indorewh1" stays that. Only a row
             * left without one falls back to a generated WH-01, so a
             * warehouse always has something to be called by.
             */
            warehouse.setCode(row[1].isEmpty()
                    ? String.format("WH-%02d", index)
                    : row[1]);
            warehouse.setName(row[0]);
            warehouse.setCity(blankToNull(form.getCity()));
            warehouse.setTimezone("Asia/Kolkata");
            warehouse.setActive(true);
            warehouseRepository.save(warehouse);
            index++;
        }

        Role adminRole = roleRepository.findByCodeIgnoreCase(SystemRole.ADMIN.name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The ADMIN role is missing from this database"));

        User admin = new User();
        admin.setCompany(saved);
        /*
         * No warehouse: an admin runs the whole company rather than one
         * site, which is what lets their app show every warehouse.
         */
        admin.setWarehouse(null);
        admin.setFullName(form.getAdminFullName().trim());
        admin.setEmail(adminEmail);
        admin.setUsername(adminEmail);
        admin.setMobileNumber(blankToNull(form.getAdminMobile()));
        admin.setPasswordHash(passwordEncoder.encode(form.getAdminPassword()));
        admin.setPasswordChangedAt(java.time.Instant.now());
        admin.setActive(true);
        /*
         * The password was typed by somebody else, so it is temporary:
         * the app holds them on "create your password" until they
         * replace it, and then nobody but them knows it.
         */
        admin.setMustChangePassword(true);
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);

        return saved.getId();
    }

    /**
     * Adds one warehouse to a company already on the system.
     *
     * A client opening a new depot should not need the company
     * re-created, and this is the only place a warehouse number can be
     * chosen - the app's own screens do not offer it.
     */
    public void addWarehouse(UUID companyId, String name, String number) {

        Company company = companyRepository.findByIdAndDeletedFalse(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (name == null || name.isBlank()) {
            throw new ConflictException("A warehouse name is required");
        }

        List<Warehouse> existing = warehouseRepository
                .findAllByCompanyIdAndDeletedFalseOrderByNameAsc(companyId);

        String code = number == null || number.isBlank()
                ? String.format("WH-%02d", existing.size() + 1)
                : number.trim();

        boolean taken = existing.stream()
                .anyMatch(w -> w.getCode().equalsIgnoreCase(code));

        if (taken) {
            throw new ConflictException(
                    "This company already has a warehouse numbered " + code);
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCompany(company);
        warehouse.setCode(code);
        warehouse.setName(name.trim());
        warehouse.setTimezone("Asia/Kolkata");
        warehouse.setActive(true);

        warehouseRepository.save(warehouse);
    }

    /**
     * Removes a warehouse from a company.
     *
     * Refused the moment anything points at it: a gate pass names the
     * site it left from, and somebody is posted to it. Both are reasons
     * the row has to stay, and both are better said out loud than
     * discovered as a foreign-key error.
     */
    public void removeWarehouse(UUID companyId, UUID warehouseId) {

        Warehouse warehouse = warehouseRepository
                .findByIdAndCompanyIdAndDeletedFalse(warehouseId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        long passes = gatePassRequestRepository
                .countByWarehouseForCompany(companyId)
                .stream()
                .filter(row -> warehouseId.equals(row[0]))
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        if (passes > 0) {
            throw new ConflictException(
                    warehouse.getName() + " has issued " + passes
                            + " gate pass(es), so it cannot be removed - "
                            + "those documents name it as the site they left "
                            + "from.");
        }

        long staff = userRepository.findAllByWarehouseIdAndDeletedFalse(warehouseId).size();

        if (staff > 0) {
            throw new ConflictException(
                    warehouse.getName() + " still has " + staff
                            + " person(s) posted to it. Move or remove them "
                            + "first.");
        }

        warehouse.setDeleted(true);
        warehouse.setDeletedAt(java.time.Instant.now());
        warehouseRepository.save(warehouse);
    }

    // ---------------------------------------------------------------
    // SUSPEND / RESUME
    // ---------------------------------------------------------------

    /**
     * Switching a company off stops every one of its people signing in,
     * without touching a single gate pass. That is the lever for a
     * client who has stopped paying; deleting their documents is not.
     */
    public boolean setActive(UUID id, boolean active) {

        Company company = companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setActive(active);
        companyRepository.save(company);

        return active;
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private boolean atWarehouse(User person, UUID warehouseId) {
        return person.getWarehouse() != null
                && person.getWarehouse().getId().equals(warehouseId);
    }

    private String roleOf(User person) {
        List<String> codes = person.getRoles().stream()
                .map(Role::getCode)
                .map(String::toUpperCase)
                .toList();

        if (codes.contains("ADMIN")) {
            return "ADMIN";
        }
        if (codes.contains("MANAGER")) {
            return "MANAGER";
        }
        return "EMPLOYEE";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
