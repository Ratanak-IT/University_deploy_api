package com.universitymanagement.curriculum.repository;

import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import com.universitymanagement.curriculum.entity.Curriculum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CurriculumRepository extends JpaRepository<Curriculum, UUID>{
    Page<Curriculum> findByProgram_Id(UUID programId, Pageable pageable);
    boolean existsByProgram_IdAndSubject_SubjectIdAndSemesterAndYearLevel(
            UUID programId, UUID subjectId, Integer semester, Integer yearLevel);
    boolean existsByProgram_IdAndSubject_SubjectIdAndSemesterAndYearLevelAndCurriculumIdNot(
            UUID programId, UUID subjectId, Integer semester, Integer yearLevel, UUID curriculumId);
}
