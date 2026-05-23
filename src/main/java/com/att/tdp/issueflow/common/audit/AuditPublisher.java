package com.att.tdp.issueflow.common.audit;

import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;

public interface AuditPublisher {

    void publish(AuditAction action, AuditEntityType entityType, Long entityId,
                 AuditActor actor, Long performedBy);
}
