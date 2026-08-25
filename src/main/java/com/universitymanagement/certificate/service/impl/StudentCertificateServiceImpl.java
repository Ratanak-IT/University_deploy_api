package com.universitymanagement.certificate.service.impl;

import com.universitymanagement.certificate.dto.response.CertificateDownloadResponse;
import com.universitymanagement.certificate.dto.response.IssuedCertificateResponse;
import com.universitymanagement.certificate.entity.IssuedCertificate;
import com.universitymanagement.certificate.entity.IssuedStatus;
import com.universitymanagement.certificate.repository.IssuedCertificateRepository;
import com.universitymanagement.certificate.service.StudentCertificateService;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.security.StudentAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCertificateServiceImpl implements StudentCertificateService {

    private final IssuedCertificateRepository issuedRepository;
    private final StudentAccessGuard accessGuard;
    private final MinioService minioService;

    @Override
    public List<IssuedCertificateResponse> getMyCertificates(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);

        return issuedRepository.findForStudent(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public String getCertificateHtml(UUID studentId, UUID issuedId) {
        IssuedCertificate certificate = requireOwned(studentId, issuedId);

        // Certificates produced from an uploaded design are PDFs, not HTML, and
        // so are scanned originals. Either way there is nothing to show here.
        if (certificate.getRenderedHtml() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This certificate is a PDF document. Download it instead.");
        }
        return certificate.getRenderedHtml();
    }

    @Override
    public CertificateDownloadResponse download(UUID studentId, UUID issuedId) {
        IssuedCertificate certificate = requireOwned(studentId, issuedId);

        if (certificate.getFileObjectName() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No file is attached to this certificate. Open it to view or print it.");
        }

        String fileName = certificate.getFileOriginalName() != null
                ? certificate.getFileOriginalName()
                : certificate.getCertificateNumber() + ".pdf";

        return new CertificateDownloadResponse(
                certificate.getIssuedId(),
                fileName,
                minioService.getAssetDownloadUrl(certificate.getFileObjectName(), fileName));
    }

    @Override
    public CertificateDownloadResponse preview(UUID studentId, UUID issuedId) {
        IssuedCertificate certificate = requireOwned(studentId, issuedId);

        if (certificate.getFileObjectName() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No file is attached to this certificate. Open it to view or print it.");
        }

        String fileName = certificate.getFileOriginalName() != null
                ? certificate.getFileOriginalName()
                : certificate.getCertificateNumber() + ".pdf";

        return new CertificateDownloadResponse(
                certificate.getIssuedId(),
                fileName,
                minioService.getAssetPreviewUrl(certificate.getFileObjectName()));
    }

    /**
     * Ownership and validity in one place.
     *
     * <p>A revoked certificate is refused rather than served: the award has been
     * withdrawn, so continuing to hand out a copy would let it keep circulating.
     */
    private IssuedCertificate requireOwned(UUID studentId, UUID issuedId) {
        accessGuard.requireSelfOrStaff(studentId);

        IssuedCertificate certificate = issuedRepository
                .findByIssuedIdAndStudent_StudentId(issuedId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Certificate not found: " + issuedId));

        if (certificate.getStatus() == IssuedStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "This certificate was revoked" + (certificate.getRevokeReason() != null
                            ? ": " + certificate.getRevokeReason() : "."));
        }
        return certificate;
    }

    private IssuedCertificateResponse toResponse(IssuedCertificate c) {
        Student student = c.getStudent();
        return new IssuedCertificateResponse(
                c.getIssuedId(),
                student != null ? student.getStudentId() : null,
                student != null ? student.getStudentCode() : null,
                student != null && student.getUser() != null
                        ? student.getUser().getFullName() : null,
                c.getCertificateType(),
                c.getCertificateNumber(),
                c.getVerificationCode(),
                c.getProgram() != null ? c.getProgram().getId() : null,
                c.getProgram() != null ? c.getProgram().getProgramName() : null,
                c.getYearLevel(),
                c.getAcademicYear(),
                c.getStatus(),
                c.getIssuedAt(),
                c.getIssuedBy(),
                c.getRevokedAt(),
                c.getRevokeReason(),
                c.getFileObjectName() != null,
                c.getRenderedHtml() != null);
    }
}
