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

    @EntityGraph(attributePaths = {"warehouse", "roles"})
    Optional<User> findByUsernameIgnoreCaseAndDeletedFalse(String username);

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
}
