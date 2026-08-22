package com.universitymanagement.attendance.repository;

import com.universitymanagement.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByStudent_StudentIdOrderByAttendanceDateDesc(UUID studentId);
    List<Attendance> findByStudent_StudentIdAndClassroom_ClassroomIdOrderByAttendanceDateDesc(UUID studentId, UUID classroomId);
    List<Attendance> findByClassroom_ClassroomIdAndAttendanceDate(UUID classroomId, LocalDate attendanceDate);
    Optional<Attendance> findByStudent_StudentIdAndClassroom_ClassroomIdAndAttendanceDate(UUID studentId, UUID classroomId, LocalDate attendanceDate);

    /** Whole register for a set of classrooms — lets attendance grades be derived in bulk. */
    List<Attendance> findByClassroom_ClassroomIdIn(List<UUID> classroomIds);
}
