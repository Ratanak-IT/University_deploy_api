package com.universitymanagement.quiz.entity;

import com.universitymanagement.auditing.BasedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestion extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /**
     * JSON array of choices, e.g. ["Option A", "Option B", "Option C", "Option D"].
     * Stored as TEXT to stay database-portable.
     */
    @Column(columnDefinition = "TEXT")
    private String optionsJson;

    /** The correct answer value (never exposed to students before grading). */
    @Column(nullable = false)
    private String correctAnswer;

    @Column(nullable = false)
    private Double score = 1.0;

    @Column(nullable = false)
    private Integer questionOrder = 0;
}
