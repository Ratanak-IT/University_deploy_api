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

    /**
     * Records that the student left the quiz screen.
     *
     * @return how many times it has now happened during this attempt
     */
    int recordFocusLoss(UUID studentId, UUID quizId, UUID attemptId);
}
