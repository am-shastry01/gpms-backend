package com.gpms.backend.gatepass.infrastructure;

import com.gpms.backend.gatepass.domain.Attachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findAllByGatePassRequestIdAndDeletedFalseOrderByCreatedAtDesc(UUID gatePassRequestId);
}
