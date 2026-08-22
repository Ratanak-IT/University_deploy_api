package com.universitymanagement.grading.entity;

import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.quiz.entity.Quiz;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single gradable item inside a component — "Quiz 1", "Midterm Exam".
 * A component may hold as many as the course needs; their points are pooled
 * before the component's weight is applied.
 *
 * <p>When {@link #assignment} or {@link #quiz} is set the scores are mirrored
 * from that module instead of being typed in, which keeps one piece of work
 * from being counted twice.
 */
@Entity
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
public class Assessment extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID assessmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private GradeComponent component;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "max_score", nullable = false)
    private Double maxScore = 100.0;

    @Column(nullable = false)
    private Integer position = 0;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    /** Set when this column mirrors an assignment's graded submissions. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    /** Set when this column mirrors a quiz's best attempt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /** Mirrored columns are read-only in the gradebook. */
    public boolean isLinked() {
        return assignment != null || quiz != null;
    }
}
