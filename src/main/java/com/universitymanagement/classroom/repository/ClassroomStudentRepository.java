package com.universitymanagement.classroom.repository;

import com.universitymanagement.classroom.entity.ClassroomStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomStudentRepository extends JpaRepository<ClassroomStudent, Long> {
    List<ClassroomStudent> findByStudent_StudentId(UUID studentId);
    boolean existsByClassroom_ClassroomIdAndStudent_StudentId(UUID classroomId, UUID studentId);
    Optional<ClassroomStudent> findByClassroom_ClassroomIdAndStudent_StudentId(UUID classroomId, UUID studentId);
    List<ClassroomStudent> findByClassroom_ClassroomId(UUID classroomId);

}
