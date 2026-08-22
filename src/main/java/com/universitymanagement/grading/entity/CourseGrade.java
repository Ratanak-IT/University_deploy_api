package com.universitymanagement.grading.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The student's grade for one course offering, stored rather than recomputed.
 *
 * <p>Once {@link CourseGradeStatus#POSTED} these numbers are frozen: later edits
 * to a score, a component weight, or the grading scale must not rewrite a
 * transcript that has already been issued.
 */
@Entity
@Table(
        name = "course_grades",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_grade_student_classroom",
                columnNames = {"student_id", "classroom_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CourseGrade extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID courseGradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    /** Weighted percentage over the components that carry data. */
    @Column(name = "score_percent")
    private Double scorePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "letter_grade", length = 10)
    private LetterGrade letterGrade;

    @Column(name = "grade_point")
    private Double gradePoint;

    /** Credits from the subject, awarded only on a passing posted grade. */
    @Column(name = "credits_earned")
    private Double creditsEarned;

    /** Share of the policy weight that actually has marks, 0–100. */
    @Column(name = "completeness_percent")
    private Double completenessPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseGradeStatus status = CourseGradeStatus.IN_PROGRESS;

    /**
     * False for a repeated course whose earlier attempt is superseded — the row
     * stays on the transcript for history but drops out of the GPA.
     */
    @Column(name = "counts_in_gpa", nullable = false)
    private Boolean countsInGpa = true;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(columnDefinition = "TEXT")
    private String remark;
}
