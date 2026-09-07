package com.universitymanagement.attendance.controller;

import com.universitymanagement.attendance.dto.request.ExcuseReviewRequest;
import com.universitymanagement.attendance.entity.AttendanceRecord;
import com.universitymanagement.attendance.entity.ExcuseApprovalStatus;
import com.universitymanagement.attendance.repository.AttendanceRecordRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.student.security.StudentAccessGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Admin review of a teacher-submitted excused-absence claim. */
@RestController
@RequestMapping("/api/v1/attendance/records/{recordId}/excuse-review")
@RequiredArgsConstructor
public class AttendanceExcuseController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentAccessGuard accessGuard;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void review(@PathVariable UUID recordId, @Valid @RequestBody ExcuseReviewRequest request) {
        AttendanceRecord record = attendanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found"));

        if (request.decision() != ExcuseApprovalStatus.APPROVED && request.decision() != ExcuseApprovalStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }

        User reviewer = accessGuard.getCurrentUser();
        record.setExcuseApprovalStatus(request.decision());
        record.setExcuseReviewedBy(reviewer);
        record.setExcuseReviewedAt(LocalDateTime.now());
        attendanceRecordRepository.save(record);
    }
}
