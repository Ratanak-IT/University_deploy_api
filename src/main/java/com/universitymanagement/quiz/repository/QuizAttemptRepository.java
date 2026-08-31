package com.universitymanagement.quiz.repository;

import com.universitymanagement.quiz.entity.AttemptStatus;
import com.universitymanagement.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    long countByQuiz_QuizIdAndStudent_StudentId(UUID quizId, UUID studentId);
    Optional<QuizAttempt> findByAttemptIdAndQuiz_QuizIdAndStudent_StudentId(UUID attemptId, UUID quizId, UUID studentId);
    List<QuizAttempt> findByStudent_StudentIdOrderByStartedAtDesc(UUID studentId);

    /**
     * Attempts that actually count against the limit and against "completed"
     * — submitted, formally expired, or still IN_PROGRESS but past its own
     * deadline. A fresh, still-active IN_PROGRESS attempt does not count: a
     * student who opens a quiz and closes the tab without submitting was
     * previously locked out of ever trying again, and the quiz showed as
     * completed to them even though they had answered nothing.
     */
    @Query("""
            select count(a) from QuizAttempt a
            where a.quiz.quizId = :quizId
              and a.student.studentId = :studentId
              and (a.status = AttemptStatus.SUBMITTED
                or a.status = AttemptStatus.EXPIRED
                or (a.status = AttemptStatus.IN_PROGRESS
                    and a.expiresAt is not null and a.expiresAt < :now))
            """)
    long countSettledByQuiz_QuizIdAndStudent_StudentId(
            @Param("quizId") UUID quizId,
            @Param("studentId") UUID studentId,
            @Param("now") LocalDateTime now);

    /**
     * An attempt still genuinely open to answer — IN_PROGRESS and either
     * undated or not yet past its deadline. Reused so starting a quiz resumes
     * this instead of silently abandoning it and creating a duplicate.
     */
    @Query("""
            select a from QuizAttempt a
            where a.quiz.quizId = :quizId
              and a.student.studentId = :studentId
              and a.status = AttemptStatus.IN_PROGRESS
              and (a.expiresAt is null or a.expiresAt >= :now)
            order by a.startedAt desc
            """)
    List<QuizAttempt> findActiveByQuiz_QuizIdAndStudent_StudentId(
            @Param("quizId") UUID quizId,
            @Param("studentId") UUID studentId,
            @Param("now") LocalDateTime now);

    /** Submitted attempts for a set of quizzes — the gradebook keeps the best one per student. */
    List<QuizAttempt> findByQuiz_QuizIdInAndStatus(List<UUID> quizIds, AttemptStatus status);

    /**
     * Every attempt on a quiz, with the student and their user row already
     * loaded — the teacher's results list reads a name off both per row.
     */
    @Query("""
            select a from QuizAttempt a
            join fetch a.student s
            left join fetch s.user u
            where a.quiz.quizId = :quizId
            order by a.startedAt desc
            """)
    List<QuizAttempt> findByQuiz_QuizIdWithStudent(@Param("quizId") UUID quizId);

    /**
     * Attempts on a quiz by students of one classroom. Pulling a quiz from a
     * section is only safe while this is zero.
     */
    @Query("""
            select count(a) from QuizAttempt a
            where a.quiz.quizId = :quizId
              and a.student.studentId in (
                  select cs.student.studentId
                  from ClassroomStudent cs
                  where cs.classroom.classroomId = :classroomId
              )
            """)
    long countByQuiz_QuizIdAndClassroom(UUID quizId, UUID classroomId);
}
