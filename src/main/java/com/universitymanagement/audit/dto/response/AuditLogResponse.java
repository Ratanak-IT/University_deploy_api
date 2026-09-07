package com.universitymanagement.audit.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID auditLogId,
        String actorName,
        String actorEmail,
        String action,
        String entityType,
        String method,
        String path,
        Integer statusCode,
        LocalDateTime createdAt
) {
}
