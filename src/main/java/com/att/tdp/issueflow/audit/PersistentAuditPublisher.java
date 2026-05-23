package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import org.springframework.stereotype.Component;

@Component
public class PersistentAuditPublisher implements AuditPublisher {

    private final AuditLogRepository auditLogRepository;

    public PersistentAuditPublisher(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void publish(AuditAction action, AuditEntityType entityType, Long entityId,
                        AuditActor actor, Long performedBy) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actor(actor)
                .performedBy(performedBy)
                .build());
    }
}
