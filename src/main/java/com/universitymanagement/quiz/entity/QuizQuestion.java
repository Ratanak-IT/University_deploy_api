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

    /**
     * Which option is correct, as a zero-based index into {@code optionsJson}.
     *
     * <p>This is the source of truth for choice questions. Matching on the
     * option's *text* — which is what {@code correctAnswer} used to be for —
     * silently breaks the moment a teacher rewords an option: the stored answer
     * no longer equals any option, and every student is then marked wrong with
     * nothing to indicate why. An index survives rewording.
     *
     * <p>Null for {@link QuestionType#SHORT_ANSWER}, which has no options.
     */
    @Column(name = "correct_option_index")
    private Integer correctOptionIndex;

    /**
     * The correct answer as text.
     *
     * <p>For SHORT_ANSWER this is the answer itself and the only source of
     * truth. For choice questions it is a denormalised mirror of the option at
     * {@code correctOptionIndex}, kept readable for exports and so the index
     * migration has something to fall back on.
     */
    @Column(nullable = false)
    private String correctAnswer;

    /**
     * Defaults to multiple choice so rows written before this column existed
     * keep working.
     *
     * <p>This used to read {@code columnDefinition = "varchar(32) not null
     * default 'MULTIPLE_CHOICE'"} so Postgres could backfill the column on
     * the table's very first {@code ADD COLUMN} — a plain {@code nullable =
     * false} with no default makes that initial add fail outright on a
     * populated table. That one-time job is done: the column exists now and
     * every row already has a value. Left in place, the same
     * {@code columnDefinition} stops being a help and starts being a problem
     * — on every later boot Hibernate's schema comparator re-diffs the
     * column against it and, for reasons particular to this Hibernate
     * version, emits {@code alter column question_type set data type
     * varchar(32) not null default '...'}, which Postgres rejects outright
     * ({@code ALTER COLUMN ... SET DATA TYPE} cannot carry a
     * {@code NOT NULL}/{@code DEFAULT} clause in the same breath — those need
     * their own {@code ALTER COLUMN} clauses). A plain {@code nullable =
     * false} column, with no {@code columnDefinition}, is what a mature
     * column should read as: nothing left for Hibernate to attempt each
     * restart, since the live schema already matches it exactly.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 32)
    private QuestionType type = QuestionType.MULTIPLE_CHOICE;

    @Column(nullable = false)
    private Double score = 1.0;

    @Column(nullable = false)
    private Integer questionOrder = 0;
}
