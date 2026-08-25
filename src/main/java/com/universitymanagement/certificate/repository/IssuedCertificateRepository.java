package com.universitymanagement.certificate.repository;

import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.entity.IssuedCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssuedCertificateRepository extends JpaRepository<IssuedCertificate, UUID> {

    /** What a student is allowed to see — nothing exists here until issued. */
    @Query("""
            select c from IssuedCertificate c
            left join fetch c.program
            where c.student.studentId = :studentId
            order by c.issuedAt desc
            """)
    List<IssuedCertificate> findForStudent(UUID studentId);

    Optional<IssuedCertificate> findByIssuedIdAndStudent_StudentId(UUID issuedId, UUID studentId);

    Optional<IssuedCertificate> findByVerificationCode(String verificationCode);

    boolean existsByStudent_StudentIdAndCertificateTypeAndAcademicYear(
            UUID studentId, CertificateType type, String academicYear);

    /** Already-awarded students, so a re-run of a batch skips them. */
    @Query("""
            select c.student.studentId from IssuedCertificate c
            where c.certificateType = :type
              and c.status = com.universitymanagement.certificate.entity.IssuedStatus.ISSUED
              and c.student.studentId in :studentIds
            """)
    List<UUID> findAlreadyIssuedStudentIds(CertificateType type, List<UUID> studentIds);

    @Query("""
            select c from IssuedCertificate c
            join fetch c.student s
            left join fetch s.user
            left join fetch c.program
            where (:type is null or c.certificateType = :type)
              and (:programId is null or c.program.id = :programId)
            order by c.issuedAt desc
            """)
    List<IssuedCertificate> search(CertificateType type, UUID programId);

    /** Next running number within a year and type. */
    @Query("""
            select count(c) from IssuedCertificate c
            where c.certificateType = :type
              and c.certificateNumber like :yearPrefix
            """)
    long countForNumbering(CertificateType type, String yearPrefix);
}
