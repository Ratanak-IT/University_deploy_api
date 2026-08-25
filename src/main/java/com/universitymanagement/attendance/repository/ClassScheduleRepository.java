package com.universitymanagement.attendance.repository;

import com.universitymanagement.attendance.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {

    List<ClassSchedule> findByClassroom_ClassroomIdOrderByDayOfWeekAscStartTimeAsc(UUID classroomId);

    /** One query for a student's whole timetable, instead of one per classroom. */
    List<ClassSchedule> findByClassroom_ClassroomIdInOrderByDayOfWeekAscStartTimeAsc(List<UUID> classroomIds);

    List<ClassSchedule> findByClassroom_ClassroomIdAndDayOfWeekOrderByStartTimeAsc(
            UUID classroomId, DayOfWeek dayOfWeek);

    void deleteByClassroom_ClassroomId(UUID classroomId);
}
