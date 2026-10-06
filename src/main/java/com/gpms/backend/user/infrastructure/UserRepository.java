package com.gpms.backend.user.infrastructure;

import com.gpms.backend.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"warehouse", "roles", "company"})
    Optional<User> findByUsernameIgnoreCaseAndDeletedFalse(String username);

    /*
     * Sign-in accepts either identifier.
     *
     * The login screen asks for an email address, while an account an
     * admin created is handed over as a username - so insisting on one
     * of the two is how a brand-new account gets told its correct
     * credentials are wrong.
     */
    /*
     * The company is fetched with it, not lazily: the sign-in check asks
     * whether the company is suspended, and that happens outside any
     * transaction - a lazy proxy there would throw instead of answering.
     */
    @EntityGraph(attributePaths = {"warehouse", "roles", "company"})
    @Query("""
            select u from User u
            where (upper(u.username) = upper(:identifier)
                or upper(u.email) = upper(:identifier))
              and u.deleted = false
            """)
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    /*
     * Deleted rows included, on purpose. Used only by the bootstrap, to
     * find a seed account that was removed in testing: the username is
     * unique across deleted rows, so inserting a fresh one would fail.
     *
     * A list rather than an Optional: one person's username can be
     * another's email, and a two-row answer must not throw inside the
     * startup path.
     */
    @EntityGraph(attributePaths = {"warehouse", "roles"})
    @Query("""
            select u from User u
            where upper(u.username) = upper(:identifier)
               or upper(u.email) = upper(:identifier)
            """)
    List<User> findByUsernameOrEmailIncludingDeleted(
            @Param("identifier") String identifier);

    @EntityGraph(attributePaths = {"warehouse", "roles"})
    Optional<User> findByIdAndDeletedFalse(UUID id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select distinct u from User u
            join u.roles r
            where upper(r.code) = upper(:roleCode)
              and u.warehouse.id = :warehouseId
              and u.active = true
              and u.deleted = false
            """)
    List<User> findActiveByRoleAndWarehouse(@Param("roleCode") String roleCode, @Param("warehouseId") UUID warehouseId);

    List<User> findAllByDeletedFalse();

    /** The people of one company, for that company's admin screens. */
    List<User> findAllByCompanyIdAndDeletedFalse(UUID companyId);

    @EntityGraph(attributePaths = {"warehouse", "roles"})
    Optional<User> findByIdAndCompanyIdAndDeletedFalse(UUID id, UUID companyId);

    /** Everyone at one site, for the warehouse overview. */
    @EntityGraph(attributePaths = {"warehouse", "roles"})
    List<User> findAllByWarehouseIdAndDeletedFalse(UUID warehouseId);
}
