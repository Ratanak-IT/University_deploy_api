package com.universitymanagement.classroom.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClassroomResponse(
        UUID classroomId,

        String className,

        String classCode,

        UUID teacherId,
        String teacherName,

        UUID subjectId,
        String subjectName,

        UUID programId,
        String programName,

        String academicYear,

        Integer semester,

        Integer yearLevel,

        String inviteCode,

        String room,

        LocalDate startDate,

        LocalDate endDate,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        String createdBy,

        String updatedBy
) {
}
