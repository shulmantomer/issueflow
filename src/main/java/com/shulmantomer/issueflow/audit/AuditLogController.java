package com.shulmantomer.issueflow.audit;

import com.shulmantomer.issueflow.audit.dto.AuditLogResponse;
import com.shulmantomer.issueflow.common.enums.AuditAction;
import com.shulmantomer.issueflow.common.enums.AuditActor;
import com.shulmantomer.issueflow.common.enums.AuditEntityType;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> search(
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditActor actor) {
        return ResponseEntity.ok(auditLogService.search(entityType, entityId, action, actor));
    }
}
