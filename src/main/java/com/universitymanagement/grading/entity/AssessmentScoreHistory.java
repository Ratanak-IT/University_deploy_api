package com.universitymanagement.grading.entity;

import com.universitymanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Snapshot of an {@link AssessmentScore}'s value immediately before an edit overwrote it. */
@Entity
@Table(name = "assessment_score_history")
@Getter
@Setter
@NoArgsConstructor
public class AssessmentScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "score_id", nullable = false)
    private AssessmentScore assessmentScore;

    private Double previousScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ScoreStatus previousStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_graded_by_teacher_id")
    private Teacher previousGradedByTeacher;

    private LocalDateTime previousGradedAt;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
