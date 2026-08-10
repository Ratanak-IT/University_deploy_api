package com.universitymanagement.attendance.service;

import com.universitymanagement.attendance.dto.request.RecordAttendanceRequest;
import com.universitymanagement.attendance.dto.response.AttendanceResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {
    List<AttendanceResponse> recordClassroomAttendance(UUID classroomId, RecordAttendanceRequest request);
    List<AttendanceResponse> getClassroomAttendanceByDate(UUID classroomId, LocalDate date);
}