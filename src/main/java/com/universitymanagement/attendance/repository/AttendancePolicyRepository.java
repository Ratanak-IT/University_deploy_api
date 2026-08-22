package com.universitymanagement.attendance.repository;

import com.universitymanagement.attendance.entity.AttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, UUID> {

    Optional<AttendancePolicy> findByClassroom_ClassroomId(UUID classroomId);

    List<AttendancePolicy> findByClassroom_ClassroomIdIn(List<UUID> classroomIds);
}
