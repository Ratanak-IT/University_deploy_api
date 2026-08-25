package com.universitymanagement.certificate.dto.response;

import com.universitymanagement.certificate.entity.CertificateType;

import java.time.LocalDateTime;

/**
 * What the public verification page shows.
 *
 * <p>Deliberately thin. Anyone holding the code can read this without logging
 * in, so it confirms the certificate is genuine and says nothing more — no
 * grades, no contact details, nothing that would turn a verification link into
 * a way to harvest student records.
 */
public record VerificationResponse(
        boolean valid,
        String message,

        String studentName,
        CertificateType certificateType,
        String certificateNumber,
        String programName,
        LocalDateTime issuedAt,
        boolean revoked
) {
    public static VerificationResponse notFound() {
        return new VerificationResponse(false,
                "No certificate matches this code.",
                null, null, null, null, null, false);
    }
}
