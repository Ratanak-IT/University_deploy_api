package com.universitymanagement.certificate.service.impl;

import com.universitymanagement.certificate.dto.request.CreateCertificateRequest;
import com.universitymanagement.certificate.dto.response.CertificateDownloadResponse;
import com.universitymanagement.certificate.dto.response.CertificateRequestResponse;
import com.universitymanagement.certificate.entity.CertificateRequest;
import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.repository.CertificateRequestRepository;
import com.universitymanagement.certificate.service.CertificateService;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.security.StudentAccessGuard;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRequestRepository certificateRequestRepository;
    private final StudentAccessGuard accessGuard;
    private final MinioService minioService;

    @Override
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

        return toResponse(certificateRequestRepository.save(entity));
    }

    @Override
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
        if (request.getFileObjectName() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Certificate file has not been uploaded yet");
        }

        String fileName = request.getFileOriginalName() != null
                ? request.getFileOriginalName()
                : "certificate-" + requestId + ".pdf";

        String url = minioService.getDownloadUrl(request.getFileObjectName(), fileName);
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
                entity.getStatus() == CertificateStatus.APPROVED && entity.getFileObjectName() != null
        );
    }
}
