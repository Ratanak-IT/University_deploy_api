package com.universitymanagement.attendance.controller;

import com.universitymanagement.attendance.dto.response.ExcuseClaimResponse;
import com.universitymanagement.attendance.entity.AttendanceRecord;
import com.universitymanagement.attendance.entity.ExcuseApprovalStatus;
import com.universitymanagement.attendance.repository.AttendanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The admin-facing queue of excused-absence claims awaiting (or already) reviewed. */
@RestController
@RequestMapping("/api/v1/attendance/excuse-claims")
@RequiredArgsConstructor
public class AttendanceExcuseClaimsController {

    private final AttendanceRecordRepository attendanceRecordRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<ExcuseClaimResponse> list(
            @RequestParam(defaultValue = "PENDING") ExcuseApprovalStatus status) {
        return attendanceRecordRepository.findByExcuseApprovalStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    private ExcuseClaimResponse toResponse(AttendanceRecord record) {
        var session = record.getSession();
        var classroom = session != null ? session.getClassroom() : null;
        var student = record.getStudent();
        String studentName = student != null && student.getUser() != null
                ? student.getUser().getFullName()
                : null;

        return new ExcuseClaimResponse(
                record.getRecordId(),
                student != null ? student.getStudentId() : null,
                studentName,
                student != null ? student.getStudentCode() : null,
                classroom != null ? classroom.getClassroomId() : null,
                classroom != null ? classroom.getClassName() : null,
                session != null ? session.getSessionDate() : null,
                record.getExcuseReference(),
                record.getRemark(),
                record.getExcuseApprovalStatus()
        );
    }
}
