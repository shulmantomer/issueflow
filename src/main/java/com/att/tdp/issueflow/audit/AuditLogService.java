package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.audit.dto.AuditLogResponse;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit log queries. Combines optional filters (entityType, entityId, action,
 * actor) via {@code Specification.allOf} — absent filters contribute null
 * predicates and are skipped, so any combination of filters works cleanly.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(AuditEntityType entityType, Long entityId,
                                         AuditAction action, AuditActor actor) {
        Specification<AuditLog> spec = Specification.allOf(
                AuditLogSpecifications.hasEntityType(entityType),
                AuditLogSpecifications.hasEntityId(entityId),
                AuditLogSpecifications.hasAction(action),
                AuditLogSpecifications.hasActor(actor));
        return auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
