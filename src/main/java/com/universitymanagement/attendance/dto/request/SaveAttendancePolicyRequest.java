package com.universitymanagement.attendance.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

/** The classroom's attendance rules, as the registrar would state them. */
public record SaveAttendancePolicyRequest(
        @DecimalMin(value = "0.0", message = "lateCredit cannot be negative")
        @DecimalMax(value = "1.0", message = "lateCredit cannot exceed 1")
        Double lateCredit,

        @PositiveOrZero(message = "lateBecomesAbsentAfterMinutes cannot be negative")
        Integer lateBecomesAbsentAfterMinutes,

        @DecimalMin(value = "0.0", message = "minPercentToSitExam cannot be negative")
        @DecimalMax(value = "100.0", message = "minPercentToSitExam cannot exceed 100")
        Double minPercentToSitExam,

        Boolean excusedAbsencesIgnored
) {
}
