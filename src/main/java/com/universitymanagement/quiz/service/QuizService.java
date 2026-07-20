package com.universitymanagement.quiz.service;

import com.universitymanagement.quiz.dto.request.AddQuizQuestionRequest;
import com.universitymanagement.quiz.dto.request.AssignQuizToClassroomRequest;
import com.universitymanagement.quiz.dto.request.CreateQuizRequest;
import com.universitymanagement.quiz.dto.response.QuizManageResponse;

import java.util.List;
import java.util.UUID;

public interface QuizService {

    QuizManageResponse createQuiz(CreateQuizRequest request);

    QuizManageResponse assignToClassroom(UUID quizId, AssignQuizToClassroomRequest request);

    QuizManageResponse addQuestion(UUID quizId, AddQuizQuestionRequest request);

    QuizManageResponse getQuiz(UUID quizId);

    List<QuizManageResponse> getMyQuizzes();
}
