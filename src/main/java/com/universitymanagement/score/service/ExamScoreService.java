package com.universitymanagement.score.service;

import com.universitymanagement.score.dto.request.SetExamScoresRequest;
import com.universitymanagement.score.dto.response.ExamScoreResponse;

import java.util.List;
import java.util.UUID;

public interface ExamScoreService {
    List<ExamScoreResponse> setScores(UUID classroomId, SetExamScoresRequest request);
    List<ExamScoreResponse> getScoresByClassroom(UUID classroomId);
}