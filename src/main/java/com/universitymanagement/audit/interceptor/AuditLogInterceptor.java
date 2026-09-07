package com.universitymanagement.audit.interceptor;

import com.universitymanagement.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Records every write request as an audit log entry, so a new admin action
 * never needs to remember to log itself — it just needs to be a
 * POST/PUT/PATCH/DELETE under `/api/v1/**`, which every mutating endpoint
 * already is.
 */
@Component
@RequiredArgsConstructor
public class AuditLogInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    /**
     * The Keycloak round trip itself isn't an "action" on a resource — logging
     * it would just fill the table with one row per login/refresh instead of
     * the CRUD activity the screen is meant to show.
     */
    private static final String[] SKIP_PREFIXES = {
            "/api/v1/auth/login",
            "/api/v1/auth/callback",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/register"
    };

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        String method = request.getMethod();
        if (!MUTATING_METHODS.contains(method)) {
            return;
        }

        String path = request.getRequestURI();
        for (String prefix : SKIP_PREFIXES) {
            if (path.startsWith(prefix)) {
                return;
            }
        }

        // Extracted here, synchronously, because the security context is
        // thread-bound: the @Async service method runs on a pooled thread
        // that never sees it.
        String actorUserId = null;
        String actorName = null;
        String actorEmail = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt jwt) {
            actorUserId = jwt.getSubject();
            actorName = jwt.getClaimAsString("preferred_username");
            actorEmail = jwt.getClaimAsString("email");
        }

        auditLogService.record(actorUserId, actorName, actorEmail, method, path, response.getStatus());
    }
}
