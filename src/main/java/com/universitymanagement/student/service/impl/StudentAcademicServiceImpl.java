package com.universitymanagement.student.service.impl;

import com.universitymanagement.assignment.dto.response.FileResponse;
import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.assignment.entity.Submission;
import com.universitymanagement.assignment.entity.SubmissionStatus;
import com.universitymanagement.assignment.repository.AssignmentRepository;
import com.universitymanagement.assignment.repository.SubmissionRepository;
import com.universitymanagement.attendance.dto.response.AttendanceResponse;
import com.universitymanagement.attendance.entity.Attendance;
import com.universitymanagement.attendance.repository.AttendanceRepository;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.student.dto.response.GpaResponse;
import com.universitymanagement.student.dto.response.GradeResponse;
import com.universitymanagement.student.dto.response.StudentAssignmentResponse;
import com.universitymanagement.student.dto.response.TranscriptResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.security.StudentAccessGuard;
import com.universitymanagement.student.service.StudentAcademicService;
import com.universitymanagement.student.util.GradeScale;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAcademicServiceImpl implements StudentAcademicService {

    private final StudentAccessGuard accessGuard;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AttendanceRepository attendanceRepository;
    private final MinioService minioService;

    @Override
    public TranscriptResponse getTranscript(UUID studentId) {
        Student student = accessGuard.requireSelfOrStaff(studentId);
        List<GradeResponse> grades = computeGrades(studentId);

        Map<String, List<GradeResponse>> byTerm = grades.stream()
                .collect(Collectors.groupingBy(
                        g -> (g.academicYear() != null ? g.academicYear() : "N/A")
                                + "|" + (g.semester() != null ? g.semester() : 0),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<TranscriptResponse.TermResponse> terms = byTerm.entrySet().stream()
                .map(entry -> {
                    String[] key = entry.getKey().split("\\|");
                    List<GradeResponse> termGrades = entry.getValue();
                    double[] gpaCredits = weightedGpa(termGrades);
                    return new TranscriptResponse.TermResponse(
                            key[0],
                            Integer.parseInt(key[1]),
                            termGrades,
                            round(gpaCredits[0]),
                            gpaCredits[1]
                    );
                })
                .sorted(Comparator
                        .comparing(TranscriptResponse.TermResponse::academicYear)
                        .thenComparing(TranscriptResponse.TermResponse::semester))
                .toList();

        double[] cumulative = weightedGpa(grades);

        return new TranscriptResponse(
                student.getStudentId(),
                student.getStudentCode(),
                student.getUser() != null ? student.getUser().getFullName() : null,
                student.getProgram() != null ? student.getProgram().getProgramName() : null,
                terms,
                round(cumulative[0]),
                cumulative[1]
        );
    }

    @Override
    public List<GradeResponse> getGrades(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);
        return computeGrades(studentId);
    }

    @Override
    public GpaResponse getGpa(UUID studentId) {
        Student student = accessGuard.requireSelfOrStaff(studentId);
        List<GradeResponse> grades = computeGrades(studentId);
        double[] cumulative = weightedGpa(grades);

        return new GpaResponse(
                student.getStudentId(),
                student.getStudentCode(),
                round(cumulative[0]),
                cumulative[1],
                grades
        );
    }

    // Attendance

    @Override
    public List<AttendanceResponse> getAttendance(UUID studentId, UUID classroomId) {
        accessGuard.requireSelfOrStaff(studentId);

        List<Attendance> records = classroomId == null
                ? attendanceRepository.findByStudent_StudentIdOrderByAttendanceDateDesc(studentId)
                : attendanceRepository
                        .findByStudent_StudentIdAndClassroom_ClassroomIdOrderByAttendanceDateDesc(
                                studentId, classroomId);

        return records.stream()
                .map(a -> new AttendanceResponse(
                        a.getAttendanceId(),
                        a.getClassroom().getClassroomId(),
                        a.getClassroom().getClassName(),
                        a.getClassroom().getSubject() != null
                                ? a.getClassroom().getSubject().getSubjectName() : null,
                        a.getAttendanceDate(),
                        a.getStatus(),
                        a.getRemark()))
                .toList();
    }

    @Override
    public List<DepartmentResponse> getDepartments(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);

        return enrolledClassrooms(studentId).stream()
                .map(Classroom::getSubject)
                .filter(Objects::nonNull)
                .map(Subject::getDepartment)
                .filter(Objects::nonNull)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .collect(Collectors.toMap(Department::getDepartmentId, d -> d, (a, b) -> a))
                .values()
                .stream()
                .map(d -> new DepartmentResponse(
                        d.getDepartmentId(),
                        d.getDepartmentName(),
                        d.getDepartmentCode(),
                        d.getIsDeleted(),
                        d.getSubjects().stream()
                                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                                .map(s -> new SubjectResponse(
                                        s.getSubjectCode(),
                                        s.getSubjectName(),
                                        s.getCredit(),
                                        d.getDepartmentId()))
                                .toList()))
                .toList();
    }

    @Override
    public List<SubjectResponse> getSubjects(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);

        return enrolledClassrooms(studentId).stream()
                .map(Classroom::getSubject)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Subject::getSubjectId, s -> s, (a, b) -> a))
                .values()
                .stream()
                .map(s -> new SubjectResponse(
                        s.getSubjectCode(),
                        s.getSubjectName(),
                        s.getCredit(),
                        s.getDepartment() != null ? s.getDepartment().getDepartmentId() : null))
                .toList();
    }

    // Assignments

    @Override
    public Page<StudentAssignmentResponse> getAssignments(UUID studentId, UUID subjectId,
                                                          String status, int page, int size) {
        accessGuard.requireSelfOrStaff(studentId);

        List<Assignment> all = enrolledClassrooms(studentId).stream()
                .filter(c -> subjectId == null
                        || (c.getSubject() != null
                            && c.getSubject().getSubjectId().equals(subjectId)))
                .flatMap(c -> assignmentRepository
                        .findByClassroom_ClassroomIdAndIsDeletedFalseOrderByDueDateAsc(
                                c.getClassroomId())
                        .stream())
                .toList();

        List<StudentAssignmentResponse> responses = all.stream()
                .map(a -> toStudentAssignmentResponse(a, studentId))
                .filter(r -> matchesStatus(r, status))
                .sorted(Comparator.comparing(StudentAssignmentResponse::dueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        int from = Math.min((int) pageable.getOffset(), responses.size());
        int to = Math.min(from + pageable.getPageSize(), responses.size());
        return new PageImpl<>(responses.subList(from, to), pageable, responses.size());
    }

    @Override
    public StudentAssignmentResponse getAssignmentDetail(UUID studentId, UUID assignmentId) {
        accessGuard.requireSelfOrStaff(studentId);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Assignment not found with id: " + assignmentId));

        boolean enrolled = classroomStudentRepository
                .existsByClassroom_ClassroomIdAndStudent_StudentId(
                        assignment.getClassroom().getClassroomId(), studentId);
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Student is not enrolled in the classroom of this assignment");
        }

        return toStudentAssignmentResponse(assignment, studentId);
    }

    // Helpers

    private List<Classroom> enrolledClassrooms(UUID studentId) {
        return classroomStudentRepository.findByStudent_StudentId(studentId)
                .stream()
                .map(ClassroomStudent::getClassroom)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .toList();
    }

    private List<GradeResponse> computeGrades(UUID studentId) {
        List<GradeResponse> grades = new ArrayList<>();

        for (Classroom classroom : enrolledClassrooms(studentId)) {
            List<Assignment> assignments = assignmentRepository
                    .findByClassroom_ClassroomIdAndIsDeletedFalseOrderByDueDateAsc(
                            classroom.getClassroomId());

            double weightedSum = 0.0;
            double weightTotal = 0.0;
            int graded = 0;

            for (Assignment assignment : assignments) {
                Optional<Submission> submission = submissionRepository
                        .findByAssignment_AssignmentIdAndStudent_StudentId(
                                assignment.getAssignmentId(), studentId);

                if (submission.isPresent()
                        && submission.get().getStatus() == SubmissionStatus.GRADED
                        && submission.get().getScore() != null
                        && assignment.getMaxScore() != null
                        && assignment.getMaxScore() > 0) {

                    double weight = assignment.getWeight() != null ? assignment.getWeight() : 1.0;
                    double percent = submission.get().getScore() / assignment.getMaxScore() * 100.0;
                    weightedSum += percent * weight;
                    weightTotal += weight;
                    graded++;
                }
            }

            Double scorePercent = weightTotal > 0 ? round(weightedSum / weightTotal) : null;
            Subject subject = classroom.getSubject();

            grades.add(new GradeResponse(
                    classroom.getClassroomId(),
                    classroom.getClassName(),
                    subject != null ? subject.getSubjectId() : null,
                    subject != null ? subject.getSubjectCode() : null,
                    subject != null ? subject.getSubjectName() : null,
                    subject != null ? subject.getCredit() : null,
                    classroom.getAcademicYear(),
                    classroom.getSemester(),
                    graded,
                    assignments.size(),
                    scorePercent,
                    scorePercent != null ? GradeScale.toLetter(scorePercent) : null,
                    scorePercent != null ? GradeScale.toGradePoint(scorePercent) : null
            ));
        }
        return grades;
    }

    private double[] weightedGpa(List<GradeResponse> grades) {
        double pointSum = 0.0;
        double creditSum = 0.0;
        for (GradeResponse grade : grades) {
            if (grade.gradePoint() == null) {
                continue;
            }
            double credit = grade.credit() != null ? grade.credit() : 1.0;
            pointSum += grade.gradePoint() * credit;
            creditSum += credit;
        }
        return new double[]{creditSum > 0 ? pointSum / creditSum : 0.0, creditSum};
    }

    private boolean matchesStatus(StudentAssignmentResponse response, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String s = status.trim().toUpperCase();
        if (s.equals("PENDING")) {
            return response.submissionId() == null;
        }
        return response.submissionStatus() != null
                && response.submissionStatus().name().equals(s);
    }

    private StudentAssignmentResponse toStudentAssignmentResponse(Assignment assignment,
                                                                  UUID studentId) {
        List<FileResponse> assignmentFiles = assignment.getFiles().stream()
                .map(f -> new FileResponse(
                        f.getFileId(),
                        f.getFileOriginalName(),
                        minioService.getPreviewUrl(f.getFileObjectName())))
                .toList();

        Submission submission = submissionRepository
                .findByAssignment_AssignmentIdAndStudent_StudentId(
                        assignment.getAssignmentId(), studentId)
                .orElse(null);

        List<FileResponse> submissionFiles = submission == null ? List.of()
                : submission.getFiles().stream()
                        .map(f -> new FileResponse(
                                f.getFileId(),
                                f.getFileOriginalName(),
                                minioService.getPreviewUrl(f.getFileObjectName())))
                        .toList();

        Classroom classroom = assignment.getClassroom();

        return new StudentAssignmentResponse(
                assignment.getAssignmentId(),
                classroom.getClassroomId(),
                classroom.getClassName(),
                classroom.getSubject() != null
                        ? classroom.getSubject().getSubjectName() : null,
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getWeight(),
                assignmentFiles,
                submission != null ? submission.getSubmissionId() : null,
                submission != null ? submission.getStatus() : null,
                submission != null ? submission.getSubmittedAt() : null,
                submission != null ? submission.getScore() : null,
                submission != null ? submission.getFeedback() : null,
                submission != null ? submission.getGradedAt() : null,
                submissionFiles
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
