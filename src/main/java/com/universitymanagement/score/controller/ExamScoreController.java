package com.universitymanagement.score.controller;

import com.universitymanagement.score.dto.request.SetExamScoresRequest;
import com.universitymanagement.score.dto.response.ExamScoreResponse;
import com.universitymanagement.score.service.ExamScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/scores")
@RequiredArgsConstructor
public class ExamScoreController {

    private final ExamScoreService examScoreService;

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping
    public List<ExamScoreResponse> setScores(@PathVariable UUID classroomId,
                                             @Valid @RequestBody SetExamScoresRequest request) {
        return examScoreService.setScores(classroomId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping
    public List<ExamScoreResponse> getScores(@PathVariable UUID classroomId) {
        return examScoreService.getScoresByClassroom(classroomId);
    }
}