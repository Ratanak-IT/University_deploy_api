package com.universitymanagement.certificate.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A certificate that has actually been awarded.
 *
 * <p>Separate from {@link CertificateRequest} on purpose: a request is someone
 * asking, this is the institution's record of what it granted. A student can
 * ask twice — for a replacement copy years later — and that is two requests
 * against one award.
 *
 * <p>A row existing here <em>is</em> the permission: the student endpoints read
 * this table, so nothing is visible or downloadable until the registrar issues
 * it. There is no separate "published" flag that could be forgotten.
 */
@Entity
@Table(
        name = "issued_certificates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_issued_certificate_number",
                        columnNames = "certificate_number"),
                @UniqueConstraint(name = "uk_issued_certificate_verification",
                        columnNames = "verification_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class IssuedCertificate extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID issuedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 40)
    private CertificateType certificateType;

    /** Human-facing reference, e.g. UT-2026-DEGREE-00042. */
    @Column(name = "certificate_number", nullable = false, length = 60)
    private String certificateNumber;

    /** What the public verification page is looked up by. */
    @Column(name = "verification_code", nullable = false, length = 40)
    private String verificationCode;

    /* ---- what produced it ---- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private CertificateTemplate template;

    /** The template can be edited afterwards; this records which version ran. */
    @Column(name = "template_version")
    private Integer templateVersion;

    /**
     * The finished document, exactly as awarded.
     *
     * <p>Re-rendering from live data on each download would quietly change the
     * certificate whenever a grade was corrected — the paper in the graduate's
     * hand would stop matching the system. What was issued is stored, and that
     * is what is served.
     */
    @Column(name = "rendered_html", columnDefinition = "TEXT")
    private String renderedHtml;

    /** Set instead of the HTML when a signed paper original was scanned in. */
    @Column(name = "file_object_name")
    private String fileObjectName;

    @Column(name = "file_original_name")
    private String fileOriginalName;

    /* ---- context at the moment of issue ---- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(name = "year_level")
    private Integer yearLevel;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    /* ---- lifecycle ---- */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuedStatus status = IssuedStatus.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column(name = "issued_by", length = 150)
    private String issuedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    public boolean isValid() {
        return status == IssuedStatus.ISSUED;
    }
}
