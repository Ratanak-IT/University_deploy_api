package com.universitymanagement.attendance.service.impl;

import com.universitymanagement.attendance.dto.request.GenerateSessionsRequest;
import com.universitymanagement.attendance.dto.request.SaveScheduleRequest;
import com.universitymanagement.attendance.dto.response.GenerateSessionsResponse;
import com.universitymanagement.attendance.dto.response.ScheduleSlotResponse;
import com.universitymanagement.attendance.entity.ClassSchedule;
import com.universitymanagement.attendance.entity.ClassSession;
import com.universitymanagement.attendance.entity.SessionType;
import com.universitymanagement.attendance.repository.ClassScheduleRepository;
import com.universitymanagement.attendance.repository.ClassSessionRepository;
import com.universitymanagement.attendance.service.ClassScheduleService;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.grading.security.ClassroomGradeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassScheduleServiceImpl implements ClassScheduleService {

    /**
     * A term is a few months; this only bites if a classroom's dates are wrong,
     * in which case failing loudly beats writing thousands of rows.
     */
    private static final int MAX_DAYS = 400;

    private final ClassScheduleRepository scheduleRepository;
    private final ClassSessionRepository sessionRepository;
    private final ClassroomGradeGuard guard;

    @Override
    public List<ScheduleSlotResponse> getSchedule(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);

        return scheduleRepository
                .findByClassroom_ClassroomIdOrderByDayOfWeekAscStartTimeAsc(classroomId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<ScheduleSlotResponse> saveSchedule(UUID classroomId, SaveScheduleRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        Set<String> seen = new HashSet<>();
        for (SaveScheduleRequest.Slot slot : request.slots()) {
            if (!seen.add(slot.dayOfWeek() + "|" + slot.startTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The timetable has two slots on " + slot.dayOfWeek()
                                + " at " + slot.startTime() + ".");
            }
            if (slot.endTime() != null && !slot.endTime().isAfter(slot.startTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A slot's end time must be after its start time ("
                                + slot.dayOfWeek() + " " + slot.startTime() + ").");
            }
        }

        // Replacing the timetable does not touch sessions already generated
        // from it — those carry marks, and a timetable edit must not rewrite
        // attendance that has already been taken.
        scheduleRepository.deleteByClassroom_ClassroomId(classroomId);
        scheduleRepository.flush();

        for (SaveScheduleRequest.Slot slot : request.slots()) {
            ClassSchedule schedule = new ClassSchedule();
            schedule.setClassroom(classroom);
            schedule.setDayOfWeek(slot.dayOfWeek());
            schedule.setStartTime(slot.startTime());
            schedule.setEndTime(slot.endTime());
            schedule.setType(slot.type() != null ? slot.type() : SessionType.LECTURE);
            schedule.setRoom(slot.room() != null ? slot.room() : classroom.getRoom());
            scheduleRepository.save(schedule);
        }

        return getSchedule(classroomId);
    }

    @Override
    @Transactional
    public GenerateSessionsResponse generateSessions(UUID classroomId,
                                                     GenerateSessionsRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        List<ClassSchedule> slots = scheduleRepository
                .findByClassroom_ClassroomIdOrderByDayOfWeekAscStartTimeAsc(classroomId);
        if (slots.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Set the weekly timetable before generating sessions.");
        }

        LocalDate from = request.from() != null ? request.from() : classroom.getStartDate();
        LocalDate to = request.to() != null ? request.to() : classroom.getEndDate();

        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This classroom has no start and end date, so give a range to generate over.");
        }
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The end date is before the start date.");
        }
        if (from.plusDays(MAX_DAYS).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That range covers more than " + MAX_DAYS
                            + " days. Check the classroom's start and end dates.");
        }

        Set<LocalDate> holidays = request.skipDates() != null
                ? new HashSet<>(request.skipDates())
                : Set.of();

        // Everything already on the calendar, so re-running only fills gaps.
        Set<String> existing = new HashSet<>();
        for (ClassSession session : sessionRepository.findInRange(classroomId, from, to)) {
            existing.add(session.getSessionDate() + "|" + session.getStartTime());
        }

        int created = 0;
        int skippedExisting = 0;
        int skippedHolidays = 0;

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (holidays.contains(date)) {
                skippedHolidays++;
                continue;
            }

            for (ClassSchedule slot : slots) {
                if (slot.getDayOfWeek() != date.getDayOfWeek()) {
                    continue;
                }

                LocalTime start = slot.getStartTime();
                if (!existing.add(date + "|" + start)) {
                    skippedExisting++;
                    continue;
                }

                ClassSession session = new ClassSession();
                session.setClassroom(classroom);
                session.setSessionDate(date);
                session.setStartTime(start);
                session.setEndTime(slot.getEndTime());
                session.setType(slot.getType());
                // Left SCHEDULED on purpose: a session only starts counting
                // once someone has actually marked the register for it.
                sessionRepository.save(session);
                created++;
            }
        }

        return new GenerateSessionsResponse(created, skippedExisting, skippedHolidays, from, to);
    }

    private ScheduleSlotResponse toResponse(ClassSchedule schedule) {
        return new ScheduleSlotResponse(
                schedule.getScheduleId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getType(),
                schedule.getRoom());
    }
}
