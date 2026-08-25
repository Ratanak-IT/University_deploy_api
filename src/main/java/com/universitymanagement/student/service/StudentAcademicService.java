package com.universitymanagement.student.service;

import com.universitymanagement.attendance.dto.response.StudentAttendanceResponse;
import com.universitymanagement.attendance.dto.response.StudentTimetableSlotResponse;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.grading.dto.response.CourseGradeResponse;
import com.universitymanagement.student.dto.response.AcademicRecordSheetResponse;
import com.universitymanagement.student.dto.response.GpaResponse;
import com.universitymanagement.student.dto.response.StudentAssignmentResponse;
import com.universitymanagement.student.dto.response.TranscriptResponse;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface StudentAcademicService {

    TranscriptResponse getTranscript(UUID studentId);

    List<CourseGradeResponse> getGrades(UUID studentId);

    GpaResponse getGpa(UUID studentId);

    Double calculateGpaInternal(UUID studentId);

    AcademicRecordSheetResponse getAcademicRecordSheet(UUID programId, Integer yearLevel,
                                                       Integer semester, String academicYear);

    /** The student's register, grouped per course, with exam eligibility. */
    List<StudentAttendanceResponse> getAttendance(UUID studentId, UUID classroomId);

    /** The weekly timetable across every classroom the student is enrolled in. */
    List<StudentTimetableSlotResponse> getTimetable(UUID studentId);

    List<DepartmentResponse> getDepartments(UUID studentId);

    List<SubjectResponse> getSubjects(UUID studentId);

    Page<StudentAssignmentResponse> getAssignments(UUID studentId, UUID subjectId,
                                                   String status, int page, int size);

    StudentAssignmentResponse getAssignmentDetail(UUID studentId, UUID assignmentId);
}