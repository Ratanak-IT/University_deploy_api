package com.universitymanagement.teacher.dto.response;

import java.util.UUID;

public record TeacherDepartmentResponse(
        UUID departmentId,
        String departmentName,
        String departmentCode
) {
}
