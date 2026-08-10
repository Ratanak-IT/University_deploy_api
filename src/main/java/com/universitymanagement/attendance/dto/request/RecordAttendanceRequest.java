package com.universitymanagement.attendance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RecordAttendanceRequest(
        @NotNull LocalDate attendanceDate,
        @NotEmpty List<@Valid AttendanceItemRequest> items
) {}
