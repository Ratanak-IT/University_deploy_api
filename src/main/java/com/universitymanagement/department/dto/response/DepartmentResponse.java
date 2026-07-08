package com.universitymanagement.department.dto.response;

import com.universitymanagement.subject.dto.response.SubjectResponse;

import java.util.List;
import java.util.UUID;

public record DepartmentResponse(
        UUID departmentId,
        String departmentName,
        List<SubjectResponse> subjects

) {
}
