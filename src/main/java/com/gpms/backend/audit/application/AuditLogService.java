package com.gpms.backend.audit.application;

import com.gpms.backend.audit.domain.AuditLog;
import com.gpms.backend.audit.infrastructure.AuditLogRepository;
import com.gpms.backend.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(
            String entityType,
            String entityId,
            String action,
            String beforeState,
            String afterState,
            String metadataJson,
            User actorUser
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setBeforeState(beforeState);
        auditLog.setAfterState(afterState);
        auditLog.setMetadataJson(metadataJson);
        auditLog.setActorUser(actorUser);
        auditLogRepository.save(auditLog);
    }
}
