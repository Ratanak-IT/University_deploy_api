package com.universitymanagement.certificate.dto.response;

import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.entity.CertificateType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A student's request, as the registrar sees it.
 *
 * <p>Carries the answer to "can I approve this?" alongside the request itself.
 * Making the registrar click Approve to find out it would fail turns a queue of
 * fifty requests into fifty round trips.
 */
public record AdminCertificateRequestResponse(
        UUID requestId,

        UUID studentId,
        String studentCode,
        String fullName,
        UUID programId,
        String programName,
        Integer yearLevel,

        CertificateType certificateType,
        String reason,
        CertificateStatus status,
        String rejectReason,

        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String processedBy,

        /* ---- what approving would do, worked out up front ---- */

        /** Why this cannot be approved, or null when it can. */
        String blockedReason,

        /** The template that would print it, so a surprise design is visible. */
        String templateName,

        /**
         * True when the block is an eligibility rule rather than a missing
         * template — the registrar may override a rule, but cannot conjure a
         * design that was never uploaded.
         */
        boolean overridable,

        /* ---- what it produced, once approved ---- */

        UUID issuedId,
        String certificateNumber,
        String verificationCode
) {
}
