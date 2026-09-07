package com.universitymanagement.audit.service.impl;

import com.universitymanagement.audit.dto.response.AuditLogResponse;
import com.universitymanagement.audit.entity.AuditAction;
import com.universitymanagement.audit.entity.AuditLog;
import com.universitymanagement.audit.repository.AuditLogRepository;
import com.universitymanagement.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Runs off the request thread — an audit row failing to write, or taking
     * a moment to write, must never slow down or break the request that
     * triggered it. Swallowing the exception here (rather than letting @Async's
     * default handler log it) keeps that guarantee explicit rather than
     * incidental.
     */
    @Override
    @Async
    public void record(String actorUserId, String actorName, String actorEmail,
                        String method, String path, int statusCode) {
        try {
            AuditLog entry = new AuditLog();
            entry.setActorUserId(actorUserId);
            entry.setActorName(actorName != null ? actorName : "Anonymous");
            entry.setActorEmail(actorEmail);
            entry.setMethod(method);
            entry.setPath(path);
            entry.setStatusCode(statusCode);
            entry.setAction(deriveAction(method));
            entry.setEntityType(deriveEntityType(path));
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to record audit log for {} {}", method, path, e);
        }
    }

    @Override
    public Page<AuditLogResponse> search(String action, String entityType, String q, Pageable pageable) {
        AuditAction parsedAction = null;
        if (StringUtils.hasText(action)) {
            try {
                parsedAction = AuditAction.valueOf(action.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // An unrecognized filter value matches nothing rather than 400ing —
                // the admin screen's dropdown is the only source of this param anyway.
            }
        }
        return auditLogRepository
                .search(parsedAction, StringUtils.hasText(entityType) ? entityType : null, q, pageable)
                .map(this::toResponse);
    }

    private AuditAction deriveAction(String method) {
        return switch (method) {
            case "POST" -> AuditAction.CREATE;
            case "PUT", "PATCH" -> AuditAction.UPDATE;
            case "DELETE" -> AuditAction.DELETE;
            default -> AuditAction.OTHER;
        };
    }

    /**
     * The path segment right after `/api/v1/` (or after `/api/v1/admin/` for
     * admin-nested routes) — a readable label, not a lookup key, so a route
     * shape nobody anticipated just yields a slightly odd label instead of an
     * error.
     */
    private String deriveEntityType(String path) {
        List<String> segments = Arrays.stream(path.split("/"))
                .filter(StringUtils::hasText)
                .toList();

        int i = 0;
        if (i < segments.size() && segments.get(i).equals("api")) i++;
        if (i < segments.size() && segments.get(i).matches("v\\d+")) i++;
        if (i < segments.size() && segments.get(i).equals("admin")) i++;

        return i < segments.size() ? segments.get(i) : "unknown";
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(
                a.getAuditLogId(),
                a.getActorName(),
                a.getActorEmail(),
                a.getAction() != null ? a.getAction().name() : null,
                a.getEntityType(),
                a.getMethod(),
                a.getPath(),
                a.getStatusCode(),
                a.getCreatedAt()
        );
    }
}
