package com.universitymanagement.department.dto.response;

import com.universitymanagement.subject.dto.response.SubjectResponse;

import java.util.List;
import java.util.UUID;

public record DepartmentResponse(
        UUID departmentId,
        String departmentName,
        String departmentCode,
        boolean isDeleted,
        List<SubjectResponse> subjects,

        /**
         * How many teachers belong to this department, and how many subjects it
         * owns.
         *
         * <p>Counted in the database rather than left for the browser to work
         * out. The admin screens previously derived these by fetching every
         * teacher and every subject and tallying them client-side, which capped
         * silently at the page size it happened to request — past that the
         * numbers were simply wrong, with nothing to say so.
         */
        long teacherCount,
        long subjectCount
) {
}
