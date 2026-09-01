package com.universitymanagement.quiz.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    /** Deadline for this attempt = startedAt + quiz.durationMinutes. */
    private LocalDateTime expiresAt;

    private Double totalScore;

    private Double earnedScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /**
     * How many times the student left the quiz screen while it was open —
     * switching tab or window, or leaving fullscreen.
     *
     * <p>Recorded, not acted on. A browser cannot stop someone alt-tabbing, and
     * pretending otherwise would be a lie told to both the student and the
     * teacher; what it can do honestly is say that it happened and how often,
     * and leave the judgement to the person marking the paper. A single blip
     * may be a notification; twenty is a different conversation.
     */
    // The SQL default matters as much as the Java one: adding a NOT NULL column
    // to a table that already has rows fails outright unless the database is
    // told what to put in them, and the field initialiser above is invisible to
    // the DDL Hibernate writes.
    @Column(name = "focus_loss_count", nullable = false, columnDefinition = "integer default 0")
    private int focusLossCount = 0;

    @Column(name = "last_focus_loss_at")
    private LocalDateTime lastFocusLossAt;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttemptAnswer> answers = new ArrayList<>();
}
