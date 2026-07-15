package com.universitymanagement.student.service;

import com.universitymanagement.attendance.dto.response.AttendanceResponse;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.student.dto.response.GpaResponse;
import com.universitymanagement.student.dto.response.GradeResponse;
import com.universitymanagement.student.dto.response.StudentAssignmentResponse;
import com.universitymanagement.student.dto.response.TranscriptResponse;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface StudentAcademicService {

    TranscriptResponse getTranscript(UUID studentId);

    List<GradeResponse> getGrades(UUID studentId);

    GpaResponse getGpa(UUID studentId);

    List<AttendanceResponse> getAttendance(UUID studentId, UUID classroomId);

    List<DepartmentResponse> getDepartments(UUID studentId);

    List<SubjectResponse> getSubjects(UUID studentId);

    Page<StudentAssignmentResponse> getAssignments(UUID studentId, UUID subjectId,
                                                   String status, int page, int size);

    StudentAssignmentResponse getAssignmentDetail(UUID studentId, UUID assignmentId);
}
