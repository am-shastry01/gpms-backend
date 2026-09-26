package com.gpms.backend.signature.infrastructure;

import com.gpms.backend.signature.domain.DigitalSignature;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalSignatureRepository extends JpaRepository<DigitalSignature, UUID> {

    /** The version currently in force for a person. */
    Optional<DigitalSignature> findByUserIdAndActiveTrueAndDeletedFalse(UUID userId);

    Optional<DigitalSignature> findByIdAndDeletedFalse(UUID id);

    /** Current signatures for everyone, for the management screen. */
    List<DigitalSignature> findAllByActiveTrueAndDeletedFalse();
}
