package com.universitymanagement.certificate.dto.response;

import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.entity.IssuedStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** An awarded certificate, as the registrar's list and the student both see it. */
public record IssuedCertificateResponse(
        UUID issuedId,
        UUID studentId,
        String studentCode,
        String fullName,

        CertificateType certificateType,
        String certificateNumber,
        String verificationCode,

        UUID programId,
        String programName,
        Integer yearLevel,
        String academicYear,

        IssuedStatus status,
        LocalDateTime issuedAt,
        String issuedBy,
        LocalDateTime revokedAt,
        String revokeReason,

        /**
         * True when a file is attached — either a scanned original, or the PDF
         * produced from an uploaded design.
         */
        boolean hasFile,

        /**
         * True when there is a rendered document to display in the browser.
         *
         * <p>Certificates made from an uploaded design are PDFs, so there is
         * nothing to show inline and the reader is sent to the download instead.
         */
        boolean hasDocument
) {
}
