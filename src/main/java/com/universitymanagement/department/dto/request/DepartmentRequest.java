package com.universitymanagement.department.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank(message = "Department name cannot be blank")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String departmentName,

        /**
         * Optional. Previously the admin form sent this and Spring dropped it on
         * the floor, because the record had no component to bind it to — the
         * text simply vanished between Save and the next page load, with
         * nothing anywhere to say why.
         */
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description
) {
}
