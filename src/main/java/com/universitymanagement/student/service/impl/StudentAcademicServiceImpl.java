package com.universitymanagement.student.service.impl;

import com.universitymanagement.assignment.dto.response.FileResponse;
import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.assignment.entity.Submission;
import com.universitymanagement.assignment.repository.AssignmentRepository;
import com.universitymanagement.assignment.repository.SubmissionRepository;
import com.universitymanagement.attendance.dto.response.StudentAttendanceResponse;
import com.universitymanagement.attendance.dto.response.StudentTimetableSlotResponse;
import com.universitymanagement.attendance.entity.ClassSchedule;
import com.universitymanagement.attendance.repository.ClassScheduleRepository;
import com.universitymanagement.attendance.service.StudentAttendanceReader;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.repository.DepartmentRepository;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.grading.calc.GpaCalculator;
import com.universitymanagement.grading.dto.response.CourseGradeResponse;
import com.universitymanagement.grading.service.CourseGradeQueryService;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.student.dto.response.AcademicRecordSheetResponse;
import com.universitymanagement.student.dto.response.GpaResponse;
import com.universitymanagement.student.dto.response.StudentAssignmentListItemResponse;
import com.universitymanagement.student.dto.response.StudentAssignmentResponse;
import com.universitymanagement.student.dto.response.StudentDashboardSummaryResponse;
import com.universitymanagement.student.dto.response.TranscriptResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.security.StudentAccessGuard;
import com.universitymanagement.student.service.StudentAcademicService;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Entirely read paths — grades, attendance, assignments, timetable. Every
 * sibling service here (certificates, cohort) already runs read-only inside
 * a transaction; this one didn't, which was invisible while open-in-view
 * kept a session alive for the whole request regardless. With that off,
 * every lazy association this class touches needs either an explicit fetch
 * join or a live session to resolve in — this covers the latter.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAcademicServiceImpl implements StudentAcademicService {

    private final StudentAccessGuard accessGuard;
    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentAttendanceReader attendanceReader;
    private final ClassScheduleRepository scheduleRepository;
    private final CourseGradeQueryService courseGradeQuery;
    private final GpaCalculator gpaCalculator;
    private final MinioService minioService;
    private final DepartmentRepository departmentRepository;

    @Override
    public TranscriptResponse getTranscript(UUID studentId) {
        Student student = accessGuard.requireSelfOrStaff(studentId);
        List<CourseGradeResponse> grades = gradesOf(studentId);

        Map<String, List<CourseGradeResponse>> byTerm = grades.stream()
                .collect(Collectors.groupingBy(
                        g -> (g.academicYear() != null ? g.academicYear() : "N/A")
                                + "|" + (g.semester() != null ? g.semester() : 0),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<TranscriptResponse.TermResponse> terms = byTerm.entrySet().stream()
                .map(entry -> {
                    String[] key = entry.getKey().split("\\|");
                    List<CourseGradeResponse> termGrades = entry.getValue();
                    GpaCalculator.Gpa termGpa = gpaCalculator.calculate(termGrades);
                    return new TranscriptResponse.TermResponse(
                            key[0],
                            Integer.parseInt(key[1]),
                            termGrades,
                            // A term's own GPA follows the same rule as the
                            // cumulative one: posted first, current as a fallback
                            // so an in-progress term is not simply blank.
                            termGpa.cumulativeGpa() != null ? termGpa.cumulativeGpa() : termGpa.currentGpa(),
                            termGpa.creditsAttempted());
                })
                .sorted(Comparator
                        .comparing(TranscriptResponse.TermResponse::academicYear)
                        .thenComparing(TranscriptResponse.TermResponse::semester))
                .toList();

        GpaCalculator.Gpa cumulative = gpaCalculator.calculate(grades);

        return new TranscriptResponse(
                student.getStudentId(),
                student.getStudentCode(),
                student.getUser() != null ? student.getUser().getFullName() : null,
                student.getProgram() != null ? student.getProgram().getProgramName() : null,
                terms,
                cumulative.cumulativeGpa(),
                cumulative.currentGpa(),
                cumulative.creditsEarned(),
                cumulative.creditsAttempted());
    }

    @Override
    public List<CourseGradeResponse> getGrades(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);
        return gradesOf(studentId);
    }

    @Override
    public Double calculateGpaInternal(UUID studentId) {
        GpaCalculator.Gpa gpa = gpaCalculator.calculate(gradesOf(studentId));
        return gpa.cumulativeGpa() != null ? gpa.cumulativeGpa() : gpa.currentGpa();
    }

    @Override
    public GpaResponse getGpa(UUID studentId) {
        Student student = accessGuard.requireSelfOrStaff(studentId);
        List<CourseGradeResponse> grades = gradesOf(studentId);
        GpaCalculator.Gpa gpa = gpaCalculator.calculate(grades);

        return new GpaResponse(
                student.getStudentId(),
                student.getStudentCode(),
                gpa.cumulativeGpa(),
                gpa.currentGpa(),
                gpa.creditsEarned(),
                gpa.creditsAttempted(),
                grades);
    }

    @Override
    public AcademicRecordSheetResponse getAcademicRecordSheet(UUID programId, Integer yearLevel,
                                                              Integer semester, String academicYear) {
        // The roster comes from who was actually enrolled in that term's
        // classrooms — not from each student's current profile fields — so a
        // past term still resolves correctly after a student has since moved
        // up a year or semester.
        List<Classroom> offerings = classroomRepository
                .findForAcademicRecordSheet(programId, yearLevel, semester,
                        (academicYear == null || academicYear.isBlank()) ? null : academicYear);

        LinkedHashMap<UUID, AcademicRecordSheetResponse.SubjectColumn> subjectsById = new LinkedHashMap<>();
        for (Classroom offering : offerings) {
            Subject subject = offering.getSubject();
            if (subject != null) {
                subjectsById.putIfAbsent(subject.getSubjectId(),
                        new AcademicRecordSheetResponse.SubjectColumn(
                                subject.getSubjectId(), subject.getSubjectCode(),
                                subject.getSubjectName(), subject.getCredit()));
            }
        }

        // One pass over the whole cohort, rather than recomputing each student's
        // entire academic history and then filtering it down to this term.
        Map<UUID, List<CourseGradeResponse>> byStudent = courseGradeQuery.gradesFor(offerings);

        List<AcademicRecordSheetResponse.StudentRow> rows = byStudent.values().stream()
                .filter(grades -> !grades.isEmpty())
                .map(grades -> {
                    CourseGradeResponse first = grades.getFirst();
                    GpaCalculator.Gpa gpa = gpaCalculator.calculate(grades);

                    List<AcademicRecordSheetResponse.GradeCell> cells = grades.stream()
                            .filter(g -> g.subjectId() != null)
                            .map(g -> new AcademicRecordSheetResponse.GradeCell(
                                    g.subjectId(),
                                    g.scorePercent(),
                                    g.letterGrade(),
                                    g.gradePoint(),
                                    g.completenessPercent(),
                                    g.status()))
                            .toList();

                    return new AcademicRecordSheetResponse.StudentRow(
                            first.studentId(),
                            first.studentCode(),
                            first.fullName(),
                            cells,
                            gpa.cumulativeGpa() != null ? gpa.cumulativeGpa() : gpa.currentGpa(),
                            gpa.creditsEarned());
                })
                .sorted(Comparator.comparing(
                        r -> r.fullName() != null ? r.fullName() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        String programName = offerings.stream()
                .map(Classroom::getProgram)
                .filter(Objects::nonNull)
                .map(Program::getProgramName)
                .findFirst()
                .orElse(null);

        return new AcademicRecordSheetResponse(
                programId,
                programName,
                yearLevel,
                semester,
                academicYear,
                new ArrayList<>(subjectsById.values()),
                rows);
    }

    // Attendance

    @Override
    public List<StudentAttendanceResponse> getAttendance(UUID studentId, UUID classroomId) {
        Student student = accessGuard.requireSelfOrStaff(studentId);
        return attendanceReader.byCourse(student, classroomId);
    }

    @Override
    public List<StudentTimetableSlotResponse> getTimetable(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);

        List<Classroom> classrooms = enrolledClassrooms(studentId);
        if (classrooms.isEmpty()) {
            return List.of();
        }

        Map<UUID, Classroom> byId = classrooms.stream()
                .collect(Collectors.toMap(Classroom::getClassroomId, c -> c));

        return scheduleRepository
                .findByClassroom_ClassroomIdInOrderByDayOfWeekAscStartTimeAsc(new ArrayList<>(byId.keySet()))
                .stream()
                .map(schedule -> toTimetableSlot(schedule, byId.get(schedule.getClassroom().getClassroomId())))
                .toList();
    }

    private StudentTimetableSlotResponse toTimetableSlot(ClassSchedule schedule, Classroom classroom) {
        Subject subject = classroom.getSubject();
        return new StudentTimetableSlotResponse(
                schedule.getScheduleId(),
                classroom.getClassroomId(),
                classroom.getClassName(),
                classroom.getClassCode(),
                subject != null ? subject.getSubjectCode() : null,
                subject != null ? subject.getSubjectName() : null,
                classroom.getTeacher() != null && classroom.getTeacher().getUser() != null
                        ? classroom.getTeacher().getUser().getFullName() : null,
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getType(),
                schedule.getRoom() != null ? schedule.getRoom() : classroom.getRoom());
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
                .map(this::toDepartmentResponse)
                .toList();
    }

    /**
     * A student sees only the departments behind the subjects they are taking,
     * so this list is a handful of rows — cheap enough to count teachers one
     * department at a time rather than reaching for a bulk query.
     */
    private DepartmentResponse toDepartmentResponse(Department d) {
        List<SubjectResponse> subjects = d.getSubjects().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .map(s -> new SubjectResponse(
                        s.getSubjectId(),
                        s.getSubjectCode(),
                        s.getSubjectName(),
                        s.getCredit(),
                        d.getDepartmentId()))
                .toList();

        return new DepartmentResponse(
                d.getDepartmentId(),
                d.getDepartmentName(),
                d.getDepartmentCode(),
                d.getDescription(),
                Boolean.TRUE.equals(d.getIsDeleted()),
                subjects,
                departmentRepository.countTeachersIn(d.getDepartmentId()),
                // Already in hand, and it is the same set the caller is shown —
                // a second query could only disagree with the list above it.
                subjects.size());
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
                        s.getSubjectId(),
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

        List<UUID> classroomIds = enrolledClassrooms(studentId).stream()
                .filter(c -> subjectId == null
                        || (c.getSubject() != null
                        && c.getSubject().getSubjectId().equals(subjectId)))
                .map(Classroom::getClassroomId)
                .toList();

        // One query for every assignment across every enrolled classroom,
        // rather than one query per classroom — and one more for every
        // submission, rather than one per assignment. A student in six
        // classes previously cost dozens of round trips here; now it costs
        // two, regardless of how many classes or assignments they have.
        List<Assignment> assignments = classroomIds.isEmpty()
                ? List.of()
                : assignmentRepository.findByClassroomIdsWithFiles(classroomIds);

        List<UUID> assignmentIds = assignments.stream().map(Assignment::getAssignmentId).toList();
        Map<UUID, Submission> submissionByAssignment = assignmentIds.isEmpty()
                ? Map.of()
                : submissionRepository.findByAssignmentIdsAndStudentWithFiles(assignmentIds, studentId)
                        .stream()
                        .collect(Collectors.toMap(s -> s.getAssignment().getAssignmentId(), s -> s));

        List<StudentAssignmentResponse> responses = assignments.stream()
                .map(a -> toStudentAssignmentResponse(a, submissionByAssignment.get(a.getAssignmentId())))
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
    public StudentDashboardSummaryResponse getDashboardSummary(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);

        List<UUID> classroomIds = enrolledClassrooms(studentId).stream()
                .map(Classroom::getClassroomId)
                .toList();

        List<Assignment> pending = classroomIds.isEmpty()
                ? List.of()
                : assignmentRepository.findPendingWithClassroomByClassroomIdsAndStudent(classroomIds, studentId);

        List<StudentDashboardSummaryResponse.UpcomingDeadline> upcoming = pending.stream()
                .limit(5)
                .map(a -> new StudentDashboardSummaryResponse.UpcomingDeadline(
                        a.getAssignmentId(),
                        a.getTitle(),
                        a.getClassroom().getClassName(),
                        a.getDueDate()))
                .toList();

        return new StudentDashboardSummaryResponse(pending.size(), upcoming);
    }

    @Override
    public List<StudentAssignmentListItemResponse> getAssignmentsList(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);

        List<UUID> classroomIds = enrolledClassrooms(studentId).stream()
                .map(Classroom::getClassroomId)
                .toList();

        List<Assignment> assignments = classroomIds.isEmpty()
                ? List.of()
                : assignmentRepository.findByClassroomIdsWithClassroomOnly(classroomIds);

        List<UUID> assignmentIds = assignments.stream().map(Assignment::getAssignmentId).toList();
        Map<UUID, Submission> submissionByAssignment = assignmentIds.isEmpty()
                ? Map.of()
                : submissionRepository.findByAssignment_AssignmentIdInAndStudent_StudentId(assignmentIds, studentId)
                        .stream()
                        .collect(Collectors.toMap(s -> s.getAssignment().getAssignmentId(), s -> s));

        return assignments.stream()
                .map(a -> {
                    Submission submission = submissionByAssignment.get(a.getAssignmentId());
                    Classroom classroom = a.getClassroom();
                    return new StudentAssignmentListItemResponse(
                            a.getAssignmentId(),
                            classroom.getClassroomId(),
                            classroom.getClassName(),
                            classroom.getSubject() != null ? classroom.getSubject().getSubjectName() : null,
                            a.getTitle(),
                            a.getDueDate(),
                            a.getMaxScore(),
                            submission != null ? submission.getStatus() : null,
                            submission != null ? submission.getScore() : null);
                })
                .toList();
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

        Submission submission = submissionRepository
                .findByAssignment_AssignmentIdAndStudent_StudentId(assignmentId, studentId)
                .orElse(null);
        return toStudentAssignmentResponse(assignment, submission);
    }

    // Helpers

    private List<CourseGradeResponse> gradesOf(UUID studentId) {
        return courseGradeQuery.gradesFor(studentId, enrolledClassrooms(studentId));
    }

    private List<Classroom> enrolledClassrooms(UUID studentId) {
        return classroomStudentRepository.findByStudentWithClassroomAndSubject(studentId)
                .stream()
                .map(ClassroomStudent::getClassroom)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .toList();
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
                                                                  Submission submission) {
        List<FileResponse> assignmentFiles = assignment.getFiles().stream()
                .map(f -> new FileResponse(
                        f.getFileId(),
                        f.getFileOriginalName(),
                        minioService.getPreviewUrl(f.getFileObjectName())))
                .toList();

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
                submissionFiles);
    }
}
