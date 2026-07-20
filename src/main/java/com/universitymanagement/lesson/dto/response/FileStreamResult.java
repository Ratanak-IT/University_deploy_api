package com.universitymanagement.lesson.dto.response;

import com.universitymanagement.minio.dto.FileStream;

public record FileStreamResult(
        FileStream file,
        String originalFileName
) {
}
