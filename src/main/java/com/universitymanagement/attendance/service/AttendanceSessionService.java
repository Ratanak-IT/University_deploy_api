package com.universitymanagement.attendance.service;

import com.universitymanagement.attendance.dto.request.CancelSessionRequest;
import com.universitymanagement.attendance.dto.request.CreateSessionRequest;
import com.universitymanagement.attendance.dto.request.MarkAttendanceRequest;
import com.universitymanagement.attendance.dto.request.SaveAttendancePolicyRequest;
import com.universitymanagement.attendance.dto.response.AttendancePolicyResponse;
import com.universitymanagement.attendance.dto.response.AttendanceSummaryResponse;
import com.universitymanagement.attendance.dto.response.OpenSessionResponse;
import com.universitymanagement.attendance.dto.response.SessionRegisterResponse;
import com.universitymanagement.attendance.dto.response.SessionResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceSessionService {

    List<SessionResponse> listSessions(UUID classroomId, LocalDate from, LocalDate to);

    SessionResponse createSession(UUID classroomId, CreateSessionRequest request);

    /**
     * The register for a date.
     *
     * <p>Opens an existing session, or creates one where that is unambiguously
     * what was meant — today, or a day the timetable says the class meets.
     * Otherwise it reports that there is nothing there, rather than inventing
     * a session for every date somebody browses to.
     */
    OpenSessionResponse openToday(UUID classroomId, LocalDate date);

    SessionRegisterResponse getRegister(UUID classroomId, UUID sessionId);

    SessionRegisterResponse markAttendance(UUID classroomId, UUID sessionId,
                                           MarkAttendanceRequest request);

    SessionResponse cancelSession(UUID classroomId, UUID sessionId, CancelSessionRequest request);

    void deleteSession(UUID classroomId, UUID sessionId);

    /** Per-student standing for the whole course, with exam eligibility. */
    List<AttendanceSummaryResponse> getSummary(UUID classroomId);

    AttendancePolicyResponse getPolicy(UUID classroomId);

    AttendancePolicyResponse savePolicy(UUID classroomId, SaveAttendancePolicyRequest request);
}
