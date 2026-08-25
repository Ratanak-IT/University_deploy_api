package com.universitymanagement.certificate.repository;

import com.universitymanagement.certificate.entity.CertificateRequest;
import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.entity.CertificateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, UUID> {
    List<CertificateRequest> findByStudent_StudentIdOrderByCreatedAtDesc(UUID studentId);
    Optional<CertificateRequest> findByRequestIdAndStudent_StudentId(UUID requestId, UUID studentId);

    /**
     * The registrar's inbox.
     *
     * <p>Joins the student, user and programme in one go: the list shows a name
     * and a programme on every row, and letting those load lazily would issue a
     * query per request.
     */
    @Query("""
            select r from CertificateRequest r
            join fetch r.student s
            left join fetch s.user u
            left join fetch s.program p
            where (:status is null or r.status = :status)
              and (:type is null or r.certificateType = :type)
              and (:programId is null or p.id = :programId)
            order by r.createdAt desc
            """)
    List<CertificateRequest> search(@Param("status") CertificateStatus status,
                                    @Param("type") CertificateType type,
                                    @Param("programId") UUID programId);
}
