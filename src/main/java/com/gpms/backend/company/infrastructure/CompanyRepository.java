package com.gpms.backend.company.infrastructure;

import com.gpms.backend.company.domain.Company;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByIdAndDeletedFalse(UUID id);

    List<Company> findAllByDeletedFalseOrderByNameAsc();

    /*
     * The first company this deployment ever had.
     *
     * By creation time, never by name: the bootstrap used to take the
     * alphabetically first company and write into it, which meant
     * onboarding a client called Acme got them a warehouse and three
     * accounts nobody asked for.
     */
    Optional<Company> findFirstByDeletedFalseOrderByCreatedAtAsc();

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);
}
