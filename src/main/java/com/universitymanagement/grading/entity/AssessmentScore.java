package com.universitymanagement.grading.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** One student's mark on one assessment. Absence of a row means "not graded yet". */
@Entity
@Table(
        name = "assessment_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assessment_score_assessment_student",
                columnNames = {"assessment_id", "student_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AssessmentScore extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Null when the status alone carries the meaning (EXCUSED). */
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScoreStatus status = ScoreStatus.GRADED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_teacher_id")
    private Teacher gradedByTeacher;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
