package com.universitymanagement.quiz.repository;

import com.universitymanagement.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {
    List<QuizQuestion> findByQuiz_QuizIdOrderByQuestionOrderAsc(UUID quizId);
}
