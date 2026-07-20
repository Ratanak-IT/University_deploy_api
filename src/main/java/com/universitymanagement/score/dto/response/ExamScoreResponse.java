package com.universitymanagement.score.dto.response;

import com.universitymanagement.score.entity.ExamType;

import java.util.UUID;

public record ExamScoreResponse(
        UUID examScoreId,
        UUID studentId,
        String studentCode,
        String studentFullName,
        UUID classroomId,
        ExamType examType,
        Double score,
        Double maxScore
) {
}