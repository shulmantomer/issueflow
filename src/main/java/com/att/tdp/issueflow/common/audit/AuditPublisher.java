package com.att.tdp.issueflow.common.audit;

import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;

/**
 * Decoupled audit hook every mutating service depends on. The {@code common}
 * package owns only this interface; the persistent implementation lives in
 * the {@code audit} feature package, keeping {@code common} free of feature
 * dependencies.
 */
public interface AuditPublisher {

    void publish(AuditAction action, AuditEntityType entityType, Long entityId,
                 AuditActor actor, Long performedBy);
}
