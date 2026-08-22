package com.universitymanagement.attendance.controller;

import com.universitymanagement.attendance.dto.request.SaveAttendancePolicyRequest;
import com.universitymanagement.attendance.dto.response.AttendancePolicyResponse;
import com.universitymanagement.attendance.dto.response.AttendanceSummaryResponse;
import com.universitymanagement.attendance.service.AttendanceSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Per-classroom attendance standing and the rules behind it. */
@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/attendance")
@RequiredArgsConstructor
public class AttendanceOverviewController {

    private final AttendanceSessionService sessionService;

    /** Each student's attendance across the course, with exam eligibility. */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<AttendanceSummaryResponse> getSummary(@PathVariable UUID classroomId) {
        return sessionService.getSummary(classroomId);
    }

    @GetMapping("/policy")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public AttendancePolicyResponse getPolicy(@PathVariable UUID classroomId) {
        return sessionService.getPolicy(classroomId);
    }

    @PutMapping("/policy")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public AttendancePolicyResponse savePolicy(
            @PathVariable UUID classroomId,
            @Valid @RequestBody SaveAttendancePolicyRequest request) {
        return sessionService.savePolicy(classroomId, request);
    }
}
