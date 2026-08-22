package com.universitymanagement.attendance.service.impl;

import com.universitymanagement.attendance.calc.AttendanceMath;
import com.universitymanagement.attendance.dto.request.CancelSessionRequest;
import com.universitymanagement.attendance.dto.request.CreateSessionRequest;
import com.universitymanagement.attendance.dto.request.MarkAttendanceRequest;
import com.universitymanagement.attendance.dto.request.SaveAttendancePolicyRequest;
import com.universitymanagement.attendance.dto.response.AttendancePolicyResponse;
import com.universitymanagement.attendance.dto.response.AttendanceSummaryResponse;
import com.universitymanagement.attendance.dto.response.OpenSessionResponse;
import com.universitymanagement.attendance.dto.response.SessionRegisterResponse;
import com.universitymanagement.attendance.dto.response.SessionResponse;
import com.universitymanagement.attendance.entity.*;
import com.universitymanagement.attendance.repository.AttendancePolicyRepository;
import com.universitymanagement.attendance.repository.AttendanceRecordRepository;
import com.universitymanagement.attendance.entity.ClassSchedule;
import com.universitymanagement.attendance.repository.ClassScheduleRepository;
import com.universitymanagement.attendance.repository.ClassSessionRepository;
import com.universitymanagement.attendance.service.AttendanceSessionService;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.grading.security.ClassroomGradeGuard;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.score.exception.StudentNotInClassroomException;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.teacher.entity.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private final ClassSessionRepository sessionRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendancePolicyRepository policyRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomGradeGuard guard;
    private final AttendanceMath math;
    private final MinioService minioService;

    // ---- sessions ----

    @Override
    public List<SessionResponse> listSessions(UUID classroomId, LocalDate from, LocalDate to) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);

        List<ClassSession> sessions = from != null && to != null
                ? sessionRepository.findInRange(classroomId, from, to)
                : sessionRepository
                .findByClassroom_ClassroomIdOrderBySessionDateDescStartTimeDesc(classroomId);

        int rosterSize = roster(classroomId).size();
        Map<UUID, List<AttendanceRecord>> bySession = recordsBySession(classroomId);

        return sessions.stream()
                .map(s -> toSessionResponse(s, rosterSize,
                        bySession.getOrDefault(s.getSessionId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public SessionResponse createSession(UUID classroomId, CreateSessionRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        Teacher teacher = guard.requireGrader(classroom);

        requireDateWithinTerm(classroom, request.sessionDate());

        sessionRepository
                .findByClassroom_ClassroomIdAndSessionDateAndStartTime(
                        classroomId, request.sessionDate(), request.startTime())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "This class already has a session at "
                                    + request.startTime() + " on " + request.sessionDate() + ".");
                });

        ClassSession session = new ClassSession();
        session.setClassroom(classroom);
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setType(request.type() != null ? request.type() : SessionType.LECTURE);
        session.setTopic(request.topic());
        session.setTakenByTeacher(teacher);

        ClassSession saved = sessionRepository.save(session);
        return toSessionResponse(saved, roster(classroomId).size(), List.of());
    }

    @Override
    @Transactional
    public OpenSessionResponse openToday(UUID classroomId, LocalDate date) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        LocalDate target = date != null ? date : LocalDate.now();
        List<ClassSession> existing = sessionRepository
                .findByClassroom_ClassroomIdAndSessionDateOrderByStartTimeAsc(classroomId, target)
                .stream()
                .filter(s -> s.getStatus() != SessionStatus.CANCELLED)
                .toList();

        if (!existing.isEmpty()) {
            // A day can hold several meetings, so open the one that is closest
            // to now rather than always the morning's.
            return OpenSessionResponse.of(
                    buildRegister(classroom, nearestTo(existing, LocalTime.now())));
        }

        List<ClassSchedule> slots = scheduleRepository
                .findByClassroom_ClassroomIdAndDayOfWeekOrderByStartTimeAsc(
                        classroomId, target.getDayOfWeek());
        List<ClassSchedule> timetable = scheduleRepository
                .findByClassroom_ClassroomIdOrderByDayOfWeekAscStartTimeAsc(classroomId);

        // Opening a day creates a session only where that is clearly what the
        // teacher meant. Browsing back through the term must not leave a trail
        // of empty registers behind it.
        if (slots.isEmpty()) {
            if (!timetable.isEmpty()) {
                return OpenSessionResponse.none(
                        "This class does not meet on " + target.getDayOfWeek().name().toLowerCase()
                                + "s. Add a session if it met anyway.");
            }
            if (!target.equals(LocalDate.now())) {
                return OpenSessionResponse.none(
                        "No session on " + target + ". Add one if the class met that day.");
            }
        }

        requireDateWithinTerm(classroom, target);

        ClassSession fresh = new ClassSession();
        fresh.setClassroom(classroom);
        fresh.setSessionDate(target);

        if (slots.isEmpty()) {
            // No timetable at all: fall back to a default slot so a classroom
            // that never set one can still take today's attendance.
            fresh.setStartTime(LocalTime.of(8, 0));
            fresh.setType(SessionType.LECTURE);
        } else {
            ClassSchedule slot = nearestSlotTo(slots, LocalTime.now());
            fresh.setStartTime(slot.getStartTime());
            fresh.setEndTime(slot.getEndTime());
            fresh.setType(slot.getType());
        }

        return OpenSessionResponse.of(
                buildRegister(classroom, sessionRepository.save(fresh)));
    }

    @Override
    public SessionRegisterResponse getRegister(UUID classroomId, UUID sessionId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);
        return buildRegister(classroom, requireSession(classroomId, sessionId));
    }

    @Override
    @Transactional
    public SessionRegisterResponse markAttendance(UUID classroomId, UUID sessionId,
                                                  MarkAttendanceRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        Teacher teacher = guard.requireGrader(classroom);

        ClassSession session = requireSession(classroomId, sessionId);
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This session was cancelled and cannot be marked. Reopen it first.");
        }

        Map<UUID, Student> roster = roster(classroomId).stream()
                .collect(Collectors.toMap(Student::getStudentId, s -> s, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();

        for (MarkAttendanceRequest.StudentMark mark : request.marks()) {
            Student student = roster.get(mark.studentId());
            if (student == null) {
                throw new StudentNotInClassroomException(mark.studentId(), classroomId);
            }

            AttendanceRecord record = recordRepository
                    .findBySession_SessionIdAndStudent_StudentId(sessionId, mark.studentId())
                    .orElseGet(AttendanceRecord::new);

            record.setSession(session);
            record.setStudent(student);
            record.setStatus(mark.status());
            record.setMinutesLate(mark.status() == AttendanceStatus.LATE ? mark.minutesLate() : null);
            record.setRemark(mark.remark());
            record.setExcuseReference(
                    mark.status() == AttendanceStatus.EXCUSED ? mark.excuseReference() : null);
            record.setRecordedByTeacher(teacher);
            record.setRecordedAt(now);

            recordRepository.save(record);
        }

        // Marking the register is what turns a scheduled slot into one that
        // counts; until then it must not drag anybody's percentage down.
        if (request.shouldMarkHeld() && session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.HELD);
        }
        session.setTakenByTeacher(teacher);
        session.setTakenAt(now);
        sessionRepository.save(session);

        return buildRegister(classroom, session);
    }

    @Override
    @Transactional
    public SessionResponse cancelSession(UUID classroomId, UUID sessionId,
                                         CancelSessionRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        ClassSession session = requireSession(classroomId, sessionId);
        session.setStatus(SessionStatus.CANCELLED);
        session.setCancellationReason(request.reason().trim());
        sessionRepository.save(session);

        return toSessionResponse(session, roster(classroomId).size(),
                recordRepository.findBySession_SessionId(sessionId));
    }

    @Override
    @Transactional
    public void deleteSession(UUID classroomId, UUID sessionId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        ClassSession session = requireSession(classroomId, sessionId);
        List<AttendanceRecord> records = recordRepository.findBySession_SessionId(sessionId);

        // Deleting a marked session would silently rewrite everyone's
        // attendance; cancelling keeps the history and the explanation.
        if (!records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This session has " + records.size()
                            + " mark(s). Cancel it instead of deleting it.");
        }
        sessionRepository.delete(session);
    }

    // ---- summary ----

    @Override
    public List<AttendanceSummaryResponse> getSummary(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);

        AttendancePolicy policy = policyFor(classroom);
        int held = (int) sessionRepository
                .countByClassroom_ClassroomIdAndStatus(classroomId, SessionStatus.HELD);

        Map<UUID, List<AttendanceRecord>> byStudent = recordRepository.findByClassroom(classroomId)
                .stream()
                .filter(r -> r.getSession().countsTowardsAttendance())
                .collect(Collectors.groupingBy(r -> r.getStudent().getStudentId()));

        return roster(classroomId).stream()
                .map(student -> {
                    AttendanceMath.Tally tally = math.tally(
                            byStudent.getOrDefault(student.getStudentId(), List.of()), held, policy);
                    Double percent = tally.percent();

                    return new AttendanceSummaryResponse(
                            student.getStudentId(),
                            student.getStudentCode(),
                            student.getUser() != null ? student.getUser().getFullName() : null,
                            avatarUrlOf(student),
                            held,
                            tally.present(),
                            tally.late(),
                            tally.absent(),
                            tally.excused(),
                            tally.unmarked(),
                            percent,
                            math.isEligibleForExam(percent, policy),
                            policy.getMinPercentToSitExam());
                })
                .toList();
    }

    // ---- policy ----

    @Override
    public AttendancePolicyResponse getPolicy(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);
        return toPolicyResponse(policyFor(classroom));
    }

    @Override
    @Transactional
    public AttendancePolicyResponse savePolicy(UUID classroomId,
                                               SaveAttendancePolicyRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        AttendancePolicy policy = policyRepository.findByClassroom_ClassroomId(classroomId)
                .orElseGet(() -> AttendancePolicy.defaultFor(classroom));

        if (request.lateCredit() != null) {
            policy.setLateCredit(request.lateCredit());
        }
        if (request.lateBecomesAbsentAfterMinutes() != null) {
            policy.setLateBecomesAbsentAfterMinutes(request.lateBecomesAbsentAfterMinutes());
        }
        if (request.minPercentToSitExam() != null) {
            policy.setMinPercentToSitExam(request.minPercentToSitExam());
        }
        if (request.excusedAbsencesIgnored() != null) {
            policy.setExcusedAbsencesIgnored(request.excusedAbsencesIgnored());
        }

        return toPolicyResponse(policyRepository.save(policy));
    }

    // ---- helpers ----

    private SessionRegisterResponse buildRegister(Classroom classroom, ClassSession session) {
        UUID classroomId = classroom.getClassroomId();
        AttendancePolicy policy = policyFor(classroom);

        Map<UUID, AttendanceRecord> marks = recordRepository
                .findBySession_SessionId(session.getSessionId())
                .stream()
                .collect(Collectors.toMap(r -> r.getStudent().getStudentId(), r -> r, (a, b) -> a));

        int held = (int) sessionRepository
                .countByClassroom_ClassroomIdAndStatus(classroomId, SessionStatus.HELD);

        Map<UUID, List<AttendanceRecord>> history = recordRepository.findByClassroom(classroomId)
                .stream()
                .filter(r -> r.getSession().countsTowardsAttendance())
                .collect(Collectors.groupingBy(r -> r.getStudent().getStudentId()));

        List<Student> roster = roster(classroomId);
        List<SessionRegisterResponse.StudentMark> students = roster.stream()
                .map(student -> {
                    AttendanceRecord mark = marks.get(student.getStudentId());
                    AttendanceMath.Tally tally = math.tally(
                            history.getOrDefault(student.getStudentId(), List.of()), held, policy);

                    return new SessionRegisterResponse.StudentMark(
                            student.getStudentId(),
                            student.getStudentCode(),
                            student.getUser() != null ? student.getUser().getFullName() : null,
                            avatarUrlOf(student),
                            mark != null ? mark.getStatus() : null,
                            mark != null ? mark.getMinutesLate() : null,
                            mark != null ? mark.getRemark() : null,
                            mark != null ? mark.getExcuseReference() : null,
                            tally.percent());
                })
                .toList();

        Subject subject = classroom.getSubject();

        return new SessionRegisterResponse(
                toSessionResponse(session, roster.size(), new ArrayList<>(marks.values())),
                classroom.getClassName(),
                subject != null ? subject.getSubjectName() : null,
                students);
    }

    private SessionResponse toSessionResponse(ClassSession session, int rosterSize,
                                              List<AttendanceRecord> records) {
        int present = 0, late = 0, absent = 0, excused = 0;
        for (AttendanceRecord record : records) {
            switch (record.getStatus()) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
            }
        }

        return new SessionResponse(
                session.getSessionId(),
                session.getClassroom().getClassroomId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getType(),
                session.getStatus(),
                session.getTopic(),
                session.getCancellationReason(),
                session.getTakenAt(),
                rosterSize,
                records.size(),
                present, late, absent, excused);
    }

    private Map<UUID, List<AttendanceRecord>> recordsBySession(UUID classroomId) {
        return recordRepository.findByClassroom(classroomId).stream()
                .collect(Collectors.groupingBy(r -> r.getSession().getSessionId()));
    }

    private List<Student> roster(UUID classroomId) {
        return classroomStudentRepository.findByClassroom_ClassroomId(classroomId).stream()
                .map(ClassroomStudent::getStudent)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        s -> s.getUser() != null && s.getUser().getFullName() != null
                                ? s.getUser().getFullName() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** The meeting whose start time sits closest to the given moment. */
    private ClassSession nearestTo(List<ClassSession> sessions, LocalTime now) {
        return sessions.stream()
                .min(Comparator.comparingLong(s ->
                        Math.abs(s.getStartTime().toSecondOfDay() - now.toSecondOfDay())))
                .orElseThrow();
    }

    private ClassSchedule nearestSlotTo(List<ClassSchedule> slots, LocalTime now) {
        return slots.stream()
                .min(Comparator.comparingLong(s ->
                        Math.abs(s.getStartTime().toSecondOfDay() - now.toSecondOfDay())))
                .orElseThrow();
    }

    private ClassSession requireSession(UUID classroomId, UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getClassroom().getClassroomId().equals(classroomId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Session not found with id: " + sessionId));
    }

    /**
     * Unsaved when a classroom has never set its own, so every read gets the
     * institutional default without writing a row on a GET.
     */
    private AttendancePolicy policyFor(Classroom classroom) {
        return policyRepository.findByClassroom_ClassroomId(classroom.getClassroomId())
                .orElseGet(() -> AttendancePolicy.defaultFor(classroom));
    }

    private void requireDateWithinTerm(Classroom classroom, LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attendance cannot be opened for a future date.");
        }
        if (classroom.getStartDate() != null && date.isBefore(classroom.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That date is before the class starts (" + classroom.getStartDate() + ").");
        }
        if (classroom.getEndDate() != null && date.isAfter(classroom.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That date is after the class ends (" + classroom.getEndDate() + ").");
        }
    }

    private String avatarUrlOf(Student student) {
        if (student.getUser() == null || student.getUser().getAvatarObjectName() == null) {
            return null;
        }
        try {
            return minioService.getAssetPreviewUrl(student.getUser().getAvatarObjectName());
        } catch (Exception e) {
            return null;
        }
    }

    private AttendancePolicyResponse toPolicyResponse(AttendancePolicy policy) {
        return new AttendancePolicyResponse(
                policy.getPolicyId(),
                policy.getClassroom().getClassroomId(),
                policy.getLateCredit(),
                policy.getLateBecomesAbsentAfterMinutes(),
                policy.getMinPercentToSitExam(),
                policy.getExcusedAbsencesIgnored());
    }
}
