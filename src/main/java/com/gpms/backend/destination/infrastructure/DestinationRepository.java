package com.gpms.backend.destination.infrastructure;

import com.gpms.backend.destination.domain.Destination;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {

    Optional<Destination> findByIdAndDeletedFalse(UUID id);

    Optional<Destination> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<Destination> findAllByDeletedFalseOrderByNameAsc();

    /**
     * Only these may appear in a dropdown. Disabling a record has to
     * take it out of circulation without deleting the history that
     * points at it.
     */
    List<Destination> findAllByDeletedFalseAndActiveTrueOrderByNameAsc();

    // ----- company-scoped (what the tenant API must use) -----

    Optional<Destination> findByIdAndCompanyIdAndDeletedFalse(UUID id, UUID companyId);

    Optional<Destination> findByNameIgnoreCaseAndCompanyIdAndDeletedFalse(String name, UUID companyId);

    List<Destination> findAllByCompanyIdAndDeletedFalseOrderByNameAsc(UUID companyId);

    List<Destination> findAllByCompanyIdAndDeletedFalseAndActiveTrueOrderByNameAsc(UUID companyId);
}
