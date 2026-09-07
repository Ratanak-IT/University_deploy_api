package com.universitymanagement.audit.repository;

import com.universitymanagement.audit.entity.AuditAction;
import com.universitymanagement.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {

    /**
     * Every filter is optional — a blank/null value passed in is treated as
     * "don't filter on this", so the one query serves the unfiltered list and
     * every combination of filters the admin screen offers.
     */
    @Query("""
            select a from AuditLog a
            where (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and (:q is null or :q = ''
                   or lower(a.actorName) like lower(concat('%', :q, '%'))
                   or lower(a.actorEmail) like lower(concat('%', :q, '%'))
                   or lower(a.path) like lower(concat('%', :q, '%')))
            """)
    Page<AuditLog> search(
            @Param("action") AuditAction action,
            @Param("entityType") String entityType,
            @Param("q") String q,
            Pageable pageable
    );
}
