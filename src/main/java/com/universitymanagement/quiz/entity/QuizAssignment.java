package com.universitymanagement.quiz.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One quiz released to one classroom.
 *
 * <p>A quiz used to hold a single {@code classroom_id}, so assigning it to a
 * second section silently moved it — the first section lost the quiz and their
 * attempts were left pointing at a paper they could no longer see. Releases
 * live here instead, one row per section.
 *
 * <p>The window can be overridden per section, because that is usually why the
 * same paper goes to several at once: two sections meet at different hours and
 * must not sit it simultaneously.
 */
@Entity
@Table(
        name = "quiz_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quiz_assignment_quiz_classroom",
                columnNames = {"quiz_id", "classroom_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class QuizAssignment extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    /** Null falls back to the quiz's own window. */
    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    public LocalDateTime effectiveFrom() {
        return availableFrom != null ? availableFrom : quiz.getStartAt();
    }

    public LocalDateTime effectiveTo() {
        return availableTo != null ? availableTo : quiz.getEndAt();
    }
}
