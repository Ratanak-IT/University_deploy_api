package com.universitymanagement.audit.service;

import com.universitymanagement.audit.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    /**
     * Fire-and-forget: called from the interceptor after every response is
     * written. Actor fields are passed in rather than read from
     * {@code SecurityContextHolder} here, because this runs {@code @Async} on
     * a pooled thread — the servlet thread's security context does not
     * follow it there.
     */
    void record(String actorUserId, String actorName, String actorEmail,
                String method, String path, int statusCode);

    Page<AuditLogResponse> search(String action, String entityType, String q, Pageable pageable);
}
