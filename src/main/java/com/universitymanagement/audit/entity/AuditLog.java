package com.universitymanagement.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One recorded write request against the API — who did it, to what, and
 * whether it actually succeeded. Rows are written by {@code AuditLogInterceptor}
 * for every POST/PUT/PATCH/DELETE, not hand-inserted per feature, so adding a
 * new admin action never requires remembering to also log it.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_created_at", columnList = "createdAt"),
                @Index(name = "idx_audit_logs_actor_user_id", columnList = "actorUserId"),
                @Index(name = "idx_audit_logs_entity_type", columnList = "entityType")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID auditLogId;

    /** The JWT subject (Keycloak user id) of whoever made the request, if any. */
    private String actorUserId;

    /** Keycloak `preferred_username` — the same identity `BaseConfig`'s AuditorAware stamps onto `createdBy`. */
    private String actorName;

    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    /**
     * The resource segment right after `/api/v1/`, e.g. "students" for
     * `/api/v1/students/{id}` — a best-effort label, not a foreign key.
     */
    private String entityType;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    private Integer statusCode;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
