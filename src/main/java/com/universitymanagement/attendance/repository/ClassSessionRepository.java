package com.universitymanagement.attendance.repository;

import com.universitymanagement.attendance.entity.ClassSession;
import com.universitymanagement.attendance.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {

    List<ClassSession> findByClassroom_ClassroomIdOrderBySessionDateDescStartTimeDesc(UUID classroomId);

    List<ClassSession> findByClassroom_ClassroomIdAndSessionDateOrderByStartTimeAsc(
            UUID classroomId, LocalDate sessionDate);

    Optional<ClassSession> findByClassroom_ClassroomIdAndSessionDateAndStartTime(
            UUID classroomId, LocalDate sessionDate, LocalTime startTime);

    List<ClassSession> findByClassroom_ClassroomIdInAndStatus(
            List<UUID> classroomIds, SessionStatus status);

    long countByClassroom_ClassroomIdAndStatus(UUID classroomId, SessionStatus status);

    /** Sessions in a date range, for the register's month view. */
    @Query("""
            select s from ClassSession s
            where s.classroom.classroomId = :classroomId
              and s.sessionDate between :from and :to
            order by s.sessionDate asc, s.startTime asc
            """)
    List<ClassSession> findInRange(UUID classroomId, LocalDate from, LocalDate to);

    /** Today's sessions still awaiting a register, across a set of classrooms — the teacher dashboard's "attendance to take" count. */
    @Query("""
            select count(s) from ClassSession s
            where s.classroom.classroomId in :classroomIds
              and s.sessionDate = :today
              and s.status = com.universitymanagement.attendance.entity.SessionStatus.SCHEDULED
            """)
    long countUntakenTodayByClassroomIds(
            @Param("classroomIds") List<UUID> classroomIds,
            @Param("today") LocalDate today);
}
