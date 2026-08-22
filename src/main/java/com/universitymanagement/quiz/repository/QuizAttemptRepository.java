package com.universitymanagement.quiz.repository;

import com.universitymanagement.quiz.entity.AttemptStatus;
import com.universitymanagement.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    long countByQuiz_QuizIdAndStudent_StudentId(UUID quizId, UUID studentId);
    Optional<QuizAttempt> findByAttemptIdAndQuiz_QuizIdAndStudent_StudentId(UUID attemptId, UUID quizId, UUID studentId);
    List<QuizAttempt> findByStudent_StudentIdOrderByStartedAtDesc(UUID studentId);

    /** Submitted attempts for a set of quizzes — the gradebook keeps the best one per student. */
    List<QuizAttempt> findByQuiz_QuizIdInAndStatus(List<UUID> quizIds, AttemptStatus status);

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
