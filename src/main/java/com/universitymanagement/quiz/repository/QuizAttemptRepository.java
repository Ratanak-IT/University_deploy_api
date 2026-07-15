package com.universitymanagement.quiz.repository;

import com.universitymanagement.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    long countByQuiz_QuizIdAndStudent_StudentId(UUID quizId, UUID studentId);
    Optional<QuizAttempt> findByAttemptIdAndQuiz_QuizIdAndStudent_StudentId(UUID attemptId, UUID quizId, UUID studentId);
    List<QuizAttempt> findByStudent_StudentIdOrderByStartedAtDesc(UUID studentId);
}
