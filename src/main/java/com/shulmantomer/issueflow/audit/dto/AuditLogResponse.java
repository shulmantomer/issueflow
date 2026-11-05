package com.shulmantomer.issueflow.audit.dto;

import com.shulmantomer.issueflow.audit.AuditLog;
import com.shulmantomer.issueflow.common.enums.AuditAction;
import com.shulmantomer.issueflow.common.enums.AuditActor;
import com.shulmantomer.issueflow.common.enums.AuditEntityType;
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
