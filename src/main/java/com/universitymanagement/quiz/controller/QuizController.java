package com.universitymanagement.quiz.controller;

import com.universitymanagement.quiz.dto.request.AddQuizQuestionRequest;
import com.universitymanagement.quiz.dto.request.AssignQuizToClassroomRequest;
import com.universitymanagement.quiz.dto.request.CreateQuizRequest;
import com.universitymanagement.quiz.dto.response.QuizManageResponse;
import com.universitymanagement.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public QuizManageResponse createQuiz(@Valid @RequestBody CreateQuizRequest request) {
        return quizService.createQuiz(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{quizId}/assign-classroom")
    public QuizManageResponse assignToClassroom(@PathVariable UUID quizId,
                                                 @Valid @RequestBody AssignQuizToClassroomRequest request) {
        return quizService.assignToClassroom(quizId, request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{quizId}/questions")
    public QuizManageResponse addQuestion(@PathVariable UUID quizId,
                                           @Valid @RequestBody AddQuizQuestionRequest request) {
        return quizService.addQuestion(quizId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/{quizId}")
    public QuizManageResponse getQuiz(@PathVariable UUID quizId) {
        return quizService.getQuiz(quizId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/mine")
    public List<QuizManageResponse> getMyQuizzes() {
        return quizService.getMyQuizzes();
    }
}
