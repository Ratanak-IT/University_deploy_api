package com.universitymanagement.lesson.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonResponse(
        UUID lessonId,
        UUID classroomId,
        String title,
        String content,
        String fileOriginalName,
        String fileUrl,
        String videoLink,
        Boolean allowDownload,
        LocalDateTime createdAt,
        String createdBy
) {
}
