package com.universitymanagement.attendance.controller;

import com.universitymanagement.attendance.dto.request.GenerateSessionsRequest;
import com.universitymanagement.attendance.dto.request.SaveScheduleRequest;
import com.universitymanagement.attendance.dto.response.GenerateSessionsResponse;
import com.universitymanagement.attendance.dto.response.ScheduleSlotResponse;
import com.universitymanagement.attendance.service.ClassScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** The weekly timetable, and laying it out across the term as sessions. */
@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/schedule")
@RequiredArgsConstructor
public class ClassScheduleController {

    private final ClassScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<ScheduleSlotResponse> getSchedule(@PathVariable UUID classroomId) {
        return scheduleService.getSchedule(classroomId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<ScheduleSlotResponse> saveSchedule(@PathVariable UUID classroomId,
                                                   @Valid @RequestBody SaveScheduleRequest request) {
        return scheduleService.saveSchedule(classroomId, request);
    }

    @PostMapping("/generate-sessions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public GenerateSessionsResponse generateSessions(
            @PathVariable UUID classroomId,
            @Valid @RequestBody GenerateSessionsRequest request) {
        return scheduleService.generateSessions(classroomId, request);
    }
}
