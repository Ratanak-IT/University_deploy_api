package com.universitymanagement.score.repository;

import com.universitymanagement.score.entity.ExamScore;
import com.universitymanagement.score.entity.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamScoreRepository extends JpaRepository<ExamScore, UUID> {

    List<ExamScore> findByStudent_StudentIdAndClassroom_ClassroomId(UUID studentId, UUID classroomId);

    List<ExamScore> findByClassroom_ClassroomId(UUID classroomId);

    List<ExamScore> findByClassroom_ClassroomIdAndExamType(UUID classroomId, ExamType examType);

    Optional<ExamScore> findByStudent_StudentIdAndClassroom_ClassroomIdAndExamType(
            UUID studentId, UUID classroomId, ExamType examType);
}