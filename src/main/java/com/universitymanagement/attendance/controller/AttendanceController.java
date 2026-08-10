package com.universitymanagement.attendance.controller;

import com.universitymanagement.attendance.dto.request.RecordAttendanceRequest;
import com.universitymanagement.attendance.dto.response.AttendanceResponse;
import com.universitymanagement.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping
    public List<AttendanceResponse> recordAttendance(@PathVariable UUID classroomId,
                                                    @Valid @RequestBody RecordAttendanceRequest request) {
        return attendanceService.recordClassroomAttendance(classroomId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping
    public List<AttendanceResponse> getAttendanceByDate(
            @PathVariable UUID classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.getClassroomAttendanceByDate(classroomId, date);
    }
}
