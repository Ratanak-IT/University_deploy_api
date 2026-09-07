package com.universitymanagement.attendance.dto.request;

import com.universitymanagement.attendance.entity.ExcuseApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record ExcuseReviewRequest(
        @NotNull ExcuseApprovalStatus decision
) {
}
