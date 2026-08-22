package com.universitymanagement.attendance.service;

import com.universitymanagement.attendance.dto.request.GenerateSessionsRequest;
import com.universitymanagement.attendance.dto.request.SaveScheduleRequest;
import com.universitymanagement.attendance.dto.response.GenerateSessionsResponse;
import com.universitymanagement.attendance.dto.response.ScheduleSlotResponse;

import java.util.List;
import java.util.UUID;

public interface ClassScheduleService {

    List<ScheduleSlotResponse> getSchedule(UUID classroomId);

    List<ScheduleSlotResponse> saveSchedule(UUID classroomId, SaveScheduleRequest request);

    /** Lays the weekly slots out across the term as scheduled sessions. */
    GenerateSessionsResponse generateSessions(UUID classroomId, GenerateSessionsRequest request);
}
