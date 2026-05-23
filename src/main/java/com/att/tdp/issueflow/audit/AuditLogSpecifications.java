package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable {@link org.springframework.data.jpa.domain.Specification}
 * fragments for {@link AuditLog} filtering. Each method returns {@code null}
 * when its filter param is absent; combined with {@code Specification.allOf},
 * null predicates are skipped, so any combination of optional filters works.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasEntityType(AuditEntityType entityType) {
        return (root, query, cb) ->
                entityType == null ? null : cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> hasEntityId(Long entityId) {
        return (root, query, cb) ->
                entityId == null ? null : cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> hasAction(AuditAction action) {
        return (root, query, cb) ->
                action == null ? null : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasActor(AuditActor actor) {
        return (root, query, cb) ->
                actor == null ? null : cb.equal(root.get("actor"), actor);
    }
}
