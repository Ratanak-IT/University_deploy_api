package com.universitymanagement.attendance.migration;

import com.universitymanagement.attendance.entity.Attendance;
import com.universitymanagement.attendance.entity.AttendanceRecord;
import com.universitymanagement.attendance.entity.ClassSession;
import com.universitymanagement.attendance.entity.SessionStatus;
import com.universitymanagement.attendance.entity.SessionType;
import com.universitymanagement.attendance.repository.AttendanceRecordRepository;
import com.universitymanagement.attendance.repository.AttendanceRepository;
import com.universitymanagement.attendance.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rebuilds the old date-based register as sessions, once, on the first start
 * after the session model ships.
 *
 * <p>Each (classroom, date) that had marks becomes one held session at 08:00 —
 * the old rows carried no time, so a single slot per day is the most the data
 * supports. Anything recorded from here on gets its real time.
 */
@Component
@RequiredArgsConstructor
@Order(120)
@Slf4j
public class LegacyAttendanceMigration implements ApplicationRunner {

    private static final LocalTime LEGACY_SLOT = LocalTime.of(8, 0);

    private final AttendanceRepository legacyRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (recordRepository.count() > 0) {
            return; // already migrated, or the register is already in use
        }

        List<Attendance> legacy = legacyRepository.findAll();
        if (legacy.isEmpty()) {
            return;
        }

        Map<String, ClassSession> sessions = new HashMap<>();
        int migrated = 0;

        for (Attendance old : legacy) {
            if (old.getClassroom() == null || old.getStudent() == null
                    || old.getAttendanceDate() == null) {
                continue;
            }

            UUID classroomId = old.getClassroom().getClassroomId();
            LocalDate date = old.getAttendanceDate();
            String key = classroomId + "|" + date;

            ClassSession session = sessions.computeIfAbsent(key, k -> {
                ClassSession fresh = new ClassSession();
                fresh.setClassroom(old.getClassroom());
                fresh.setSessionDate(date);
                fresh.setStartTime(LEGACY_SLOT);
                fresh.setType(SessionType.LECTURE);
                // It was marked, so it happened — and it should keep counting.
                fresh.setStatus(SessionStatus.HELD);
                fresh.setTopic("Imported from the previous register");
                return sessionRepository.save(fresh);
            });

            AttendanceRecord record = new AttendanceRecord();
            record.setSession(session);
            record.setStudent(old.getStudent());
            record.setStatus(old.getStatus());
            record.setRemark(old.getRemark());
            record.setRecordedAt(old.getLastUpdateAt() != null
                    ? old.getLastUpdateAt() : old.getCreatedAt());

            recordRepository.save(record);
            migrated++;
        }

        log.info("Legacy attendance migration: {} mark(s) moved into {} session(s).",
                migrated, sessions.size());
    }
}
