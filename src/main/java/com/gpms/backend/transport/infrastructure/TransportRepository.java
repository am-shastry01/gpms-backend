package com.gpms.backend.transport.infrastructure;

import com.gpms.backend.transport.domain.Transport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportRepository extends JpaRepository<Transport, UUID> {

    Optional<Transport> findByIdAndDeletedFalse(UUID id);

    Optional<Transport> findByCodeIgnoreCaseAndDeletedFalse(String code);

    Optional<Transport> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<Transport> findAllByDeletedFalseOrderByNameAsc();

    /**
     * Only these may appear in a dropdown. Disabling a record has to
     * take it out of circulation without deleting the history that
     * points at it.
     */
    List<Transport> findAllByDeletedFalseAndActiveTrueOrderByNameAsc();

    // ----- company-scoped (what the tenant API must use) -----

    Optional<Transport> findByIdAndCompanyIdAndDeletedFalse(UUID id, UUID companyId);

    Optional<Transport> findByCodeIgnoreCaseAndCompanyIdAndDeletedFalse(String code, UUID companyId);

    Optional<Transport> findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(String name, UUID companyId);

    List<Transport> findAllByCompanyIdAndDeletedFalseOrderByNameAsc(UUID companyId);

    List<Transport> findAllByCompanyIdAndDeletedFalseAndActiveTrueOrderByNameAsc(UUID companyId);
}
