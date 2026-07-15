package com.universitymanagement.quiz.service;

import com.universitymanagement.quiz.dto.request.SubmitQuizAttemptRequest;
import com.universitymanagement.quiz.dto.response.QuizAttemptResponse;
import com.universitymanagement.quiz.dto.response.QuizResponse;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptService {
    List<QuizResponse> getQuizzesForStudent(UUID studentId);
    QuizAttemptResponse startAttempt(UUID studentId, UUID quizId);
    QuizAttemptResponse submitAttempt(UUID studentId, UUID quizId, UUID attemptId, SubmitQuizAttemptRequest request);
    QuizAttemptResponse getAttemptResult(UUID studentId, UUID quizId, UUID attemptId);
}
