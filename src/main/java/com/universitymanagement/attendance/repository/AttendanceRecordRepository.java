package com.universitymanagement.attendance.repository;

import com.universitymanagement.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findBySession_SessionId(UUID sessionId);

    Optional<AttendanceRecord> findBySession_SessionIdAndStudent_StudentId(UUID sessionId, UUID studentId);

    long countBySession_SessionId(UUID sessionId);

    /** Every mark in a classroom, with its session — one query for the whole register. */
    @Query("""
            select r from AttendanceRecord r
            join fetch r.session s
            where s.classroom.classroomId = :classroomId
            """)
    List<AttendanceRecord> findByClassroom(UUID classroomId);

    /**
     * Marks across a set of classrooms, for deriving attendance grades in bulk.
     * Only held sessions come back, since nothing else counts.
     */
    @Query("""
            select r from AttendanceRecord r
            join fetch r.session s
            where s.classroom.classroomId in :classroomIds
              and s.status = com.universitymanagement.attendance.entity.SessionStatus.HELD
            """)
    List<AttendanceRecord> findHeldByClassrooms(List<UUID> classroomIds);

    @Query("""
            select r from AttendanceRecord r
            join fetch r.session s
            left join fetch s.classroom c
            left join fetch c.subject
            where r.student.studentId = :studentId
              and (:classroomId is null or c.classroomId = :classroomId)
            order by s.sessionDate desc, s.startTime desc
            """)
    List<AttendanceRecord> findForStudent(UUID studentId, UUID classroomId);
}
