package com.gpms.backend.gatepass.infrastructure;

import com.gpms.backend.gatepass.domain.GatePassItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatePassItemRepository extends JpaRepository<GatePassItem, UUID> {

    List<GatePassItem> findAllByGatePassRequestIdAndDeletedFalseOrderByLineNumberAsc(UUID gatePassRequestId);
}
