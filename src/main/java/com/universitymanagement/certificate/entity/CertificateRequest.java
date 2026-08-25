package com.universitymanagement.certificate.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificate_requests")
@Getter
@Setter
@NoArgsConstructor
public class CertificateRequest extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CertificateType certificateType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateStatus status = CertificateStatus.PENDING;

    /** MinIO object name of the approved certificate file (private bucket). */
    @Column(name = "file_object_name")
    private String fileObjectName;

    @Column(name = "file_original_name")
    private String fileOriginalName;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "processed_by")
    private String processedBy;

    /**
     * The certificate this request produced.
     *
     * <p>Approving does not attach a file by hand — it issues through the same
     * path a cohort batch uses, so a requested certificate carries the same
     * number, verification code and template version as any other. Without
     * that, an approved request would produce a document that cannot be
     * verified, which is not a certificate at all.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_certificate_id")
    private IssuedCertificate issuedCertificate;
}
