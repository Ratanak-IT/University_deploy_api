package com.universitymanagement.certificate.repository;

import com.universitymanagement.certificate.entity.CertificateTemplate;
import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.entity.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {

    List<CertificateTemplate> findByOrderByCertificateTypeAscNameAsc();

    List<CertificateTemplate> findByCertificateTypeAndStatus(CertificateType type, TemplateStatus status);

    /**
     * The template issuing should use: the programme's own if it has one,
     * otherwise the general fallback. Ordered so the specific one comes first.
     */
    @Query("""
            select t from CertificateTemplate t
            where t.certificateType = :type
              and t.status = com.universitymanagement.certificate.entity.TemplateStatus.ACTIVE
              and (t.program is null or t.program.id = :programId)
            order by case when t.program is null then 1 else 0 end asc
            """)
    List<CertificateTemplate> findActiveFor(CertificateType type, UUID programId);

    Optional<CertificateTemplate> findFirstByCertificateTypeAndProgramIsNullAndStatus(
            CertificateType type, TemplateStatus status);
}
