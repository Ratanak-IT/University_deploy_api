package com.universitymanagement.department.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank(message = "Department name cannot be blank")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String departmentName
) {
}
