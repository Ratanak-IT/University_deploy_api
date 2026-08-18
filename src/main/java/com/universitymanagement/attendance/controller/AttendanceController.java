package com.universitymanagement.attendance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitymanagement.attendance.dto.request.AttendanceItemRequest;
import com.universitymanagement.attendance.dto.request.RecordAttendanceRequest;
import com.universitymanagement.attendance.dto.response.AttendanceResponse;
import com.universitymanagement.attendance.entity.AttendanceStatus;
import com.universitymanagement.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ObjectMapper objectMapper;

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping
    public List<AttendanceResponse> recordAttendance(
            @PathVariable UUID classroomId,
            @RequestBody String rawJson
    ) {
        RecordAttendanceRequest request = parseRecordAttendanceRequest(rawJson);
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

    private RecordAttendanceRequest parseRecordAttendanceRequest(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            // 1. Extract date
            String dateStr = null;
            if (root.hasNonNull("attendanceDate")) {
                dateStr = root.get("attendanceDate").asText();
            } else if (root.hasNonNull("date")) {
                dateStr = root.get("date").asText();
            }

            LocalDate attendanceDate = LocalDate.now();
            if (dateStr != null && !dateStr.isBlank()) {
                String cleanDate = dateStr.trim();
                if (cleanDate.contains("T")) {
                    cleanDate = cleanDate.split("T")[0];
                } else if (cleanDate.contains(" ")) {
                    cleanDate = cleanDate.split(" ")[0];
                }
                attendanceDate = LocalDate.parse(cleanDate);
            }

            // 2. Extract items array
            JsonNode itemsNode = null;
            if (root.has("items") && root.get("items").isArray()) {
                itemsNode = root.get("items");
            } else if (root.has("records") && root.get("records").isArray()) {
                itemsNode = root.get("records");
            } else if (root.has("students") && root.get("students").isArray()) {
                itemsNode = root.get("students");
            }

            List<AttendanceItemRequest> items = new ArrayList<>();
            if (itemsNode != null) {
                for (JsonNode node : itemsNode) {
                    // Extract studentId
                    String idStr = null;
                    if (node.hasNonNull("studentId")) {
                        idStr = node.get("studentId").asText();
                    } else if (node.hasNonNull("id")) {
                        idStr = node.get("id").asText();
                    } else if (node.hasNonNull("userId")) {
                        idStr = node.get("userId").asText();
                    }

                    if (idStr == null || idStr.isBlank()) {
                        continue;
                    }

                    UUID studentId = UUID.fromString(idStr.trim());

                    // Extract status
                    String statusStr = null;
                    if (node.hasNonNull("status")) {
                        statusStr = node.get("status").asText();
                    } else if (node.hasNonNull("attendanceStatus")) {
                        statusStr = node.get("attendanceStatus").asText();
                    }

                    AttendanceStatus status = parseStatus(statusStr);

                    // Extract remark
                    String remark = null;
                    if (node.hasNonNull("remark")) {
                        remark = node.get("remark").asText();
                    } else if (node.hasNonNull("note")) {
                        remark = node.get("note").asText();
                    }

                    items.add(new AttendanceItemRequest(studentId, status, remark));
                }
            }

            return new RecordAttendanceRequest(attendanceDate, items);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid attendance payload: " + e.getMessage(), e);
        }
    }

    private AttendanceStatus parseStatus(String input) {
        if (input == null || input.isBlank()) {
            return AttendanceStatus.PRESENT;
        }
        String upper = input.trim().toUpperCase();
        for (AttendanceStatus s : AttendanceStatus.values()) {
            if (s.name().equals(upper)) {
                return s;
            }
        }
        if (upper.contains("ABSENT") || upper.contains("ABS")) return AttendanceStatus.ABSENT;
        if (upper.contains("LATE")) return AttendanceStatus.LATE;
        if (upper.contains("EXCUSED") || upper.contains("PERMIT") || upper.contains("LEAVE")) return AttendanceStatus.EXCUSED;
        return AttendanceStatus.PRESENT;
    }
}
