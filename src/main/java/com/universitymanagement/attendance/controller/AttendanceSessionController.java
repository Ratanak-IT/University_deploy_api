package com.universitymanagement.attendance.controller;

import com.universitymanagement.attendance.dto.request.CancelSessionRequest;
import com.universitymanagement.attendance.dto.request.CreateSessionRequest;
import com.universitymanagement.attendance.dto.request.MarkAttendanceRequest;
import com.universitymanagement.attendance.dto.request.SaveAttendancePolicyRequest;
import com.universitymanagement.attendance.dto.response.AttendancePolicyResponse;
import com.universitymanagement.attendance.dto.response.AttendanceSummaryResponse;
import com.universitymanagement.attendance.dto.response.OpenSessionResponse;
import com.universitymanagement.attendance.dto.response.SessionRegisterResponse;
import com.universitymanagement.attendance.dto.response.SessionResponse;
import com.universitymanagement.attendance.service.AttendanceSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The session-based attendance API.
 *
 * <p>Everything here goes through validated DTOs. The older endpoint read the
 * request body as a raw string and guessed at its shape, which bypassed
 * validation entirely and defaulted an unreadable status to PRESENT.
 */
@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/sessions")
@RequiredArgsConstructor
public class AttendanceSessionController {

    private final AttendanceSessionService sessionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<SessionResponse> listSessions(
            @PathVariable UUID classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return sessionService.listSessions(classroomId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public SessionResponse createSession(@PathVariable UUID classroomId,
                                         @Valid @RequestBody CreateSessionRequest request) {
        return sessionService.createSession(classroomId, request);
    }

    /**
     * Opens (or reuses) a date's session and returns its register in one call.
     * Reports {@code opened: false} for a day the class does not meet.
     */
    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public OpenSessionResponse openToday(
            @PathVariable UUID classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return sessionService.openToday(classroomId, date);
    }

    @GetMapping("/{sessionId}/register")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public SessionRegisterResponse getRegister(@PathVariable UUID classroomId,
                                               @PathVariable UUID sessionId) {
        return sessionService.getRegister(classroomId, sessionId);
    }

    @PostMapping("/{sessionId}/register")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public SessionRegisterResponse markAttendance(@PathVariable UUID classroomId,
                                                  @PathVariable UUID sessionId,
                                                  @Valid @RequestBody MarkAttendanceRequest request) {
        return sessionService.markAttendance(classroomId, sessionId, request);
    }

    @PostMapping("/{sessionId}/cancel")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public SessionResponse cancelSession(@PathVariable UUID classroomId,
                                         @PathVariable UUID sessionId,
                                         @Valid @RequestBody CancelSessionRequest request) {
        return sessionService.cancelSession(classroomId, sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public void deleteSession(@PathVariable UUID classroomId, @PathVariable UUID sessionId) {
        sessionService.deleteSession(classroomId, sessionId);
    }
}
