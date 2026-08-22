package com.universitymanagement.attendance.dto.response;

import java.util.UUID;

public record AttendancePolicyResponse(
        UUID policyId,
        UUID classroomId,
        Double lateCredit,
        Integer lateBecomesAbsentAfterMinutes,
        Double minPercentToSitExam,
        Boolean excusedAbsencesIgnored
) {
}
