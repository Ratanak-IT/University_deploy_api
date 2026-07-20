package com.universitymanagement.lesson.dto.response;

import java.util.UUID;

public record LessonFileResponse(
        UUID fileId,
        String fileOriginalName,
        String previewUrl
) {
}
