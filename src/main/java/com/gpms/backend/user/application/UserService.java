package com.gpms.backend.user.application;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.common.service.CurrentUserService;
import com.gpms.backend.company.domain.Company;
import com.gpms.backend.user.api.dto.ResetPasswordRequest;
import com.gpms.backend.user.api.dto.UserDeleteResponse;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final CurrentUserService currentUserService;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            WarehouseRepository warehouseRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.warehouseRepository = warehouseRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    /*
     * The company's own people. An admin manages their own staff and
     * nobody else's - this used to answer with every user in the
     * database, which with more than one client on it is the whole
     * directory.
     */
    public List<UserResponse> getAll() {
        return userRepository.findAllByCompanyIdAndDeletedFalse(companyId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse create(UserCreateRequest request) {

        /*
         * The email is the sign-in name unless a username was asked for
         * explicitly. One unique thing instead of two: nobody has to
         * invent a free handle, and two companies can both employ a Ravi
         * without colliding.
         */
        String username = request.username() == null || request.username().isBlank()
                ? request.email().trim().toLowerCase()
                : request.username().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException(
                    "That email address is already in use");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException(
                    "That email address is already in use as a sign-in name");
        }
        Company company = company();
        Warehouse warehouse = resolveWarehouse(company, request.warehouseId());
        User user = new User();
        user.setUsername(username);
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordChangedAt(java.time.Instant.now());
        user.setFullName(request.fullName().trim());
        user.setMobileNumber(request.mobileNumber());
        user.setCompany(company);
        user.setWarehouse(warehouse);
        user.setActive(request.active() == null || request.active());
        user.setRoles(resolveRoles(request.roles()));
        /*
         * An admin typed this password on somebody else's behalf, so
         * it is temporary: the app holds the person on the
         * "create a new password" screen until they replace it.
         */
        user.setMustChangePassword(true);
        return toResponse(userRepository.save(user));
    }

    /*
     * Issues a fresh temporary password.
     *
     * The must-change flag goes back up with it, so a reset password is
     * never a password the admin knows for long: the person replaces it
     * the next time they sign in.
     */
    public UserResponse resetPassword(UUID id, ResetPasswordRequest request) {
        User user = userRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setMustChangePassword(true);
        /*
         * A reset ends whatever sessions the old password had open -
         * which is the point of resetting it.
         */
        user.setPasswordChangedAt(java.time.Instant.now());
        return toResponse(userRepository.save(user));
    }

    /*
     * Removes a person for real - the row leaves the users table.
     *
     * There is one case where it cannot: a gate pass that has already
     * been issued points at the people who raised and signed it, and a
     * gate pass is a document somebody may have to produce months
     * later. Deleting that person would either break the document or
     * silently erase who authorised it, so when a pass refers to them
     * the account is disabled instead and the admin is told why.
     *
     * Everything that is only about them - their roles, their
     * notifications, their stored signature images, their name on an
     * audit entry - goes with them. Deletes are ordered children
     * first, because the foreign keys are the real check here and the
     * database is the thing enforcing it.
     */
    public UserDeleteResponse delete(UUID id) {

        User user = userRepository.findByIdAndCompanyIdAndDeletedFalse(id, companyId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (appearsOnAGatePass(id)) {

            user.setActive(false);
            user.setDeleted(true);
            user.setDeletedAt(Instant.now());
            userRepository.save(user);

            return new UserDeleteResponse(
                    false,
                    user.getFullName() + " signed or raised gate passes that have "
                            + "already been issued, so the account was disabled "
                            + "instead of deleted. They can no longer sign in, and "
                            + "those documents still show who authorised them.");
        }

        /*
         * Pending work is written off: a draft nobody can act on, and
         * notifications addressed to an account that is going away.
         */
        execute("delete from notifications where recipient_user_id = :id", id);
        execute("update audit_logs set actor_user_id = null where actor_user_id = :id", id);
        execute("delete from digital_signatures where user_id = :id", id);
        execute("delete from user_roles where user_id = :id", id);
        execute("delete from users where id = :id", id);

        /*
         * The deletes above were native, so the entity this method
         * loaded is still sitting in the persistence context pointing
         * at a row that no longer exists. Clearing it stops Hibernate
         * flushing that ghost back out on the way home.
         */
        entityManager.clear();

        return new UserDeleteResponse(
                true,
                user.getFullName() + " was deleted.");
    }

    /*
     * Every column in the schema that points a gate pass at a person.
     * If any of them names this user, the account stays.
     */
    private boolean appearsOnAGatePass(UUID id) {

        Number count = (Number) entityManager.createNativeQuery("""
                select
                    (select count(*) from gate_pass_requests g
                      where g.requested_by_user_id = :id
                         or g.approved_by_user_id = :id
                         or g.exited_by_user_id = :id
                         or g.entered_by_user_id = :id
                         or g.manager_approved_by_user_id = :id
                         or g.admin_approved_by_user_id = :id
                         or g.employee_approved_by_user_id = :id)
                  + (select count(*) from approvals a
                      where a.manager_user_id = :id)
                  + (select count(*) from attachments t
                      where t.uploaded_by_user_id = :id)
                  + (select count(*) from gate_pass_requests g
                      join digital_signatures s
                        on s.id in (g.manager_signature_id,
                                    g.admin_signature_id,
                                    g.requester_signature_id,
                                    g.employee_signature_id)
                      where s.user_id = :id)
                """)
                .setParameter("id", id)
                .getSingleResult();

        return count != null && count.longValue() > 0;
    }

    private void execute(String sql, UUID id) {
        entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .executeUpdate();
    }

    /*
     * The column is NOT NULL, so a created user always lands in a
     * warehouse. The caller may name one; an app that does not - the
     * admin screen, which has no warehouse picker - gets the only
     * warehouse there is, which is the single-site case this system
     * actually runs in.
     */
    /*
     * The site this person works at, inside the admin's own company.
     *
     * A named warehouse is looked up scoped, so an id from another
     * client resolves to nothing. No warehouse named means a
     * company-wide account - an admin, who is not posted to one site -
     * except when the company has exactly one, which is then the
     * obvious answer.
     */
    private Warehouse resolveWarehouse(Company company, UUID warehouseId) {

        if (warehouseId != null) {
            return warehouseRepository
                    .findByIdAndCompanyIdAndDeletedFalse(warehouseId, company.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        }

        List<Warehouse> own = warehouseRepository
                .findAllByCompanyIdAndDeletedFalseOrderByNameAsc(company.getId());

        return own.size() == 1 ? own.get(0) : null;
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
                user.getWarehouse() == null ? null : user.getWarehouse().getId(),
                user.getWarehouse() == null ? null : user.getWarehouse().getName(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.isMustChangePassword(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Company company() {
        return currentUserService.requireCurrentUser().getCompany();
    }

    private UUID companyId() {
        return company().getId();
    }
}
