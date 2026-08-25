package com.universitymanagement.certificate.dto.response;

import java.util.List;

/**
 * What a batch actually did.
 *
 * <p>Reported per student rather than as a single count: issuing to a cohort
 * silently skips people for several different reasons, and a registrar needs to
 * see which ones and why before telling a student their certificate is ready.
 */
public record IssueBatchResponse(
        int issued,
        int skipped,
        List<Skipped> skippedStudents,
        List<IssuedCertificateResponse> certificates
) {
    public record Skipped(String studentCode, String fullName, String reason) {
    }
}
