package com.universitymanagement.certificate.repository;

import com.universitymanagement.certificate.entity.CertificateRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, UUID> {
    List<CertificateRequest> findByStudent_StudentIdOrderByCreatedAtDesc(UUID studentId);
    Optional<CertificateRequest> findByRequestIdAndStudent_StudentId(UUID requestId, UUID studentId);
}
