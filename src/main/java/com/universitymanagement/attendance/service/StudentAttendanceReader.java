package com.universitymanagement.attendance.service;

import com.universitymanagement.attendance.calc.AttendanceMath;
import com.universitymanagement.attendance.dto.response.AttendanceResponse;
import com.universitymanagement.attendance.dto.response.StudentAttendanceResponse;
import com.universitymanagement.attendance.entity.AttendancePolicy;
import com.universitymanagement.attendance.entity.AttendanceRecord;
import com.universitymanagement.attendance.entity.ClassSession;
import com.universitymanagement.attendance.entity.SessionStatus;
import com.universitymanagement.attendance.repository.AttendancePolicyRepository;
import com.universitymanagement.attendance.repository.AttendanceRecordRepository;
import com.universitymanagement.attendance.repository.ClassSessionRepository;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.subject.entity.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read side of the register for one student, shared by the student portal and
 * the admin screens.
 *
 * <p>It applies each classroom's own policy through {@link AttendanceMath}, so
 * the figure a student sees is the same one the gradebook used.
 */
@Service
@RequiredArgsConstructor
public class StudentAttendanceReader {

    private final AttendanceRecordRepository recordRepository;
    private final AttendancePolicyRepository policyRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceMath math;

    @Transactional(readOnly = true)
    public List<StudentAttendanceResponse> byCourse(Student student, UUID classroomId) {
        List<AttendanceRecord> records =
                recordRepository.findForStudent(student.getStudentId(), classroomId);

        // Group by classroom, keeping the order the query returned (most
        // recent session first) so the current course leads.
        Map<UUID, List<AttendanceRecord>> byClassroom = new LinkedHashMap<>();
        Map<UUID, Classroom> classrooms = new HashMap<>();

        for (AttendanceRecord record : records) {
            ClassSession session = record.getSession();
            if (session == null || session.getClassroom() == null) {
                continue;
            }
            Classroom classroom = session.getClassroom();
            classrooms.putIfAbsent(classroom.getClassroomId(), classroom);
            byClassroom
                    .computeIfAbsent(classroom.getClassroomId(), k -> new ArrayList<>())
                    .add(record);
        }

        List<StudentAttendanceResponse> responses = new ArrayList<>();

        for (Map.Entry<UUID, List<AttendanceRecord>> entry : byClassroom.entrySet()) {
            Classroom classroom = classrooms.get(entry.getKey());
            AttendancePolicy policy = policyRepository
                    .findByClassroom_ClassroomId(entry.getKey())
                    .orElseGet(() -> AttendancePolicy.defaultFor(classroom));

            int held = (int) sessionRepository
                    .countByClassroom_ClassroomIdAndStatus(entry.getKey(), SessionStatus.HELD);

            // Only held sessions count; a cancelled class still appears in the
            // history below so the student can see why the gap is there.
            List<AttendanceRecord> counted = entry.getValue().stream()
                    .filter(r -> r.getSession().countsTowardsAttendance())
                    .toList();

            AttendanceMath.Tally tally = math.tally(counted, held, policy);
            Double percent = tally.percent();
            Subject subject = classroom.getSubject();

            responses.add(new StudentAttendanceResponse(
                    classroom.getClassroomId(),
                    classroom.getClassName(),
                    subject != null ? subject.getSubjectCode() : null,
                    subject != null ? subject.getSubjectName() : null,
                    classroom.getAcademicYear(),
                    classroom.getSemester(),
                    held,
                    tally.present(),
                    tally.late(),
                    tally.absent(),
                    tally.excused(),
                    tally.unmarked(),
                    percent,
                    math.isEligibleForExam(percent, policy),
                    policy.getMinPercentToSitExam(),
                    entry.getValue().stream().map(r -> toResponse(r, student)).toList()));
        }

        return responses;
    }

    private AttendanceResponse toResponse(AttendanceRecord record, Student student) {
        ClassSession session = record.getSession();
        Classroom classroom = session.getClassroom();
        Subject subject = classroom != null ? classroom.getSubject() : null;

        return new AttendanceResponse(
                record.getRecordId(),
                session.getSessionId(),
                classroom != null ? classroom.getClassroomId() : null,
                classroom != null ? classroom.getClassName() : null,
                subject != null ? subject.getSubjectName() : null,
                student.getStudentId(),
                student.getStudentCode(),
                student.getUser() != null ? student.getUser().getFullName() : null,
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getType(),
                session.getTopic(),
                record.getStatus(),
                record.getMinutesLate(),
                record.getRemark(),
                record.getExcuseReference());
    }
}
