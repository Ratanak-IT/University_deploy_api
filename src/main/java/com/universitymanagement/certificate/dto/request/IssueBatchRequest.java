package com.universitymanagement.certificate.dto.request;

import com.universitymanagement.certificate.entity.CertificateType;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Awards a certificate to a whole cohort at once — the normal way a graduating
 * year is handled, rather than one student at a time.
 */
public record IssueBatchRequest(
        @NotNull(message = "certificateType is required")
        CertificateType certificateType,

        @NotNull(message = "programId is required")
        UUID programId,

        Integer yearLevel,
        String academicYear,

        /**
         * Restrict to these students. Null issues to everyone in the cohort who
         * qualifies — the usual case for a graduating year.
         */
        List<UUID> studentIds,

        /**
         * Issue even to students the eligibility check rejects. Requires an
         * explicit choice, so an under-credited student is never awarded a
         * degree by accident.
         */
        Boolean overrideEligibility
) {
    public boolean shouldOverride() {
        return Boolean.TRUE.equals(overrideEligibility);
    }
}
