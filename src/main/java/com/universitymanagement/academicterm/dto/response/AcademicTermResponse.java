package com.universitymanagement.academicterm.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicTermResponse(
        UUID termId,
        String name,
        String academicYear,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate addDropDeadline,
        Boolean isActive
) {
}
