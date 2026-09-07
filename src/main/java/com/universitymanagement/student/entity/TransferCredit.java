package com.universitymanagement.student.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * External credit granted toward a student's completion total. Kept as its own
 * ledger rather than a {@link com.universitymanagement.grading.entity.CourseGrade}
 * row — it has no grade point and must never enter the GPA calculation.
 */
@Entity
@Table(name = "transfer_credits")
@Getter
@Setter
@NoArgsConstructor
public class TransferCredit extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID transferCreditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private String sourceInstitution;

    @Column(nullable = false)
    private Double credits;

    private LocalDate grantedDate;
}
