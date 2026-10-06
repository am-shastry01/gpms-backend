package com.gpms.backend.company.infrastructure;

import com.gpms.backend.company.domain.Company;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByIdAndDeletedFalse(UUID id);

    List<Company> findAllByDeletedFalseOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);
}
