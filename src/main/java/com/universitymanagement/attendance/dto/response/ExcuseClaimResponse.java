package com.universitymanagement.attendance.dto.response;

import com.universitymanagement.attendance.entity.ExcuseApprovalStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ExcuseClaimResponse(
        UUID recordId,
        UUID studentId,
        String studentName,
        String studentCode,
        UUID classroomId,
        String className,
        LocalDate sessionDate,
        String excuseReference,
        String remark,
        ExcuseApprovalStatus excuseApprovalStatus
) {
}
