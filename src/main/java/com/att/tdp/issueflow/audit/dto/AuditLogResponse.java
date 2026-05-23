package com.att.tdp.issueflow.audit.dto;

import com.att.tdp.issueflow.audit.AuditLog;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        AuditAction action,
        AuditEntityType entityType,
        Long entityId,
        Long performedBy,
        AuditActor actor,
        Instant timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getAction(), log.getEntityType(),
                log.getEntityId(), log.getPerformedBy(), log.getActor(), log.getTimestamp());
    }
}
