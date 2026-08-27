package com.gpms.backend.gatepass.infrastructure;

import com.gpms.backend.gatepass.domain.Approval;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findAllByGatePassRequestIdAndDeletedFalseOrderByActionTimeDesc(UUID gatePassRequestId);
}
