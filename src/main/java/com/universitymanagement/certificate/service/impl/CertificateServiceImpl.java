package com.universitymanagement.certificate.service.impl;

import com.universitymanagement.certificate.dto.request.CreateCertificateRequest;
import com.universitymanagement.certificate.dto.response.CertificateDownloadResponse;
import com.universitymanagement.certificate.dto.response.CertificateRequestResponse;
import com.universitymanagement.certificate.entity.CertificateRequest;
import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.entity.IssuedCertificate;
import com.universitymanagement.certificate.entity.IssuedStatus;
import com.universitymanagement.certificate.repository.CertificateRequestRepository;
import com.universitymanagement.certificate.service.CertificateService;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.security.StudentAccessGuard;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import com.universitymanagement.notification.service.NotificationService;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRequestRepository certificateRequestRepository;
    private final StudentAccessGuard accessGuard;
    private final MinioService minioService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<CertificateRequestResponse> getRequestsForStudent(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);
        return certificateRequestRepository
                .findByStudent_StudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CertificateRequestResponse createRequest(UUID studentId, CreateCertificateRequest request) {
        // Only the student themself can request a certificate
        Student student = accessGuard.requireSelf(studentId);

        CertificateRequest entity = new CertificateRequest();
        entity.setStudent(student);
        entity.setCertificateType(request.certificateType());
        entity.setReason(request.reason());
        entity.setStatus(CertificateStatus.PENDING);

        CertificateRequest saved = certificateRequestRepository.save(entity);

        // Real notification trigger
        if (student.getUser() != null) {
            notificationService.createNotification(
                    student.getUser().getId(),
                    "Certificate Request Submitted",
                    "Your request for " + request.certificateType().toString().replace("_", " ") + " Certificate has been submitted successfully.",
                    "CERTIFICATE",
                    "Official Certificate Application",
                    "Academic Registrar",
                    // Previously this used the 6-arg overload with no link at
                    // all, so clicking the notification did nothing.
                    "/dashboard/student/certificates",
                    "CERTIFICATE",
                    saved.getRequestId()
            );
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateDownloadResponse downloadApprovedCertificate(UUID studentId, UUID requestId) {
        accessGuard.requireSelfOrStaff(studentId);

        CertificateRequest request = certificateRequestRepository
                .findByRequestIdAndStudent_StudentId(requestId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Certificate request not found: " + requestId));

        if (request.getStatus() != CertificateStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Certificate request is not approved yet (status: " + request.getStatus() + ")");
        }

        // Approving issues a real certificate, so the file to hand over is that
        // one. fileObjectName is the older path, where a registrar attached a
        // scan by hand; it is still honoured so historic requests keep working.
        IssuedCertificate issued = request.getIssuedCertificate();
        String objectName = issued != null && issued.getFileObjectName() != null
                ? issued.getFileObjectName()
                : request.getFileObjectName();

        if (objectName == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This certificate has no downloadable file. Open it from "
                            + "My Certificates instead.");
        }

        if (issued != null && issued.getStatus() == IssuedStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "This certificate was revoked" + (issued.getRevokeReason() != null
                            ? ": " + issued.getRevokeReason() : "."));
        }

        String fileName = issued != null
                ? issued.getCertificateNumber() + ".pdf"
                : (request.getFileOriginalName() != null
                        ? request.getFileOriginalName()
                        : "certificate-" + requestId + ".pdf");

        String url = minioService.getAssetDownloadUrl(objectName, fileName);
        return new CertificateDownloadResponse(requestId, fileName, url);
    }

    private CertificateRequestResponse toResponse(CertificateRequest entity) {
        return new CertificateRequestResponse(
                entity.getRequestId(),
                entity.getCertificateType(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRejectReason(),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getStatus() == CertificateStatus.APPROVED
                        && (entity.getFileObjectName() != null
                                || (entity.getIssuedCertificate() != null
                                        && entity.getIssuedCertificate().getFileObjectName() != null))
        );
    }
}
