package com.universitymanagement.lesson.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LessonResponse(
        UUID lessonId,
        UUID classroomId,
        String title,
        String content,
        List<LessonFileResponse> files,
        String videoLink,
        Boolean allowDownload,
        LocalDateTime createdAt,
        String createdBy
) {
}
