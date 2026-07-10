package com.universitymanagement.teacher.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateTeacherRequest(
        String specialization,
        List<UUID> departmentIds,
        String position,
        LocalDate hireDate,
        String employmentStatus
) {
}
