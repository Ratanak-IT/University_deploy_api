package com.universitymanagement.attendance.dto.request;

import com.universitymanagement.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceItemRequest(
        @NotNull UUID studentId,
        @NotNull AttendanceStatus status,
        String remark
) {}
