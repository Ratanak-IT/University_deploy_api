package com.universitymanagement.curriculum.repository;

import com.universitymanagement.curriculum.entity.Curriculum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CurriculumRepository extends JpaRepository<Curriculum, UUID> {
    Page<Curriculum> findByProgram_Id(UUID programId, Pageable pageable);
    
    List<Curriculum> findByProgram_IdAndIsDeletedFalseOrderByYearLevelAscSemesterAsc(UUID programId);

    boolean existsByProgram_IdAndSubject_SubjectIdAndSemesterAndYearLevel(
            UUID programId, UUID subjectId, Integer semester, Integer yearLevel);

    boolean existsByProgram_IdAndSubject_SubjectIdAndSemesterAndYearLevelAndCurriculumIdNot(
            UUID programId, UUID subjectId, Integer semester, Integer yearLevel, UUID curriculumId);

    /** How many distinct subjects a program's curriculum covers. */
    interface ProgramSubjectCount {
        UUID getProgramId();

        long getTotal();
    }

    /**
     * Counts every program in one query. The list screen needs a number per row,
     * and counting per row would issue a query for each program on the page.
     *
     * <p>A subject scheduled in two terms is still one subject, hence the distinct.
     */
    @Query("""
            select c.program.id as programId, count(distinct c.subject.subjectId) as total
            from Curriculum c
            where c.isDeleted = false
              and c.program is not null
              and c.subject is not null
            group by c.program.id
            """)
    List<ProgramSubjectCount> countSubjectsByProgram();

    /** One row per subject a programme requires, with its credit value. */
    interface ProgramSubjectCredit {
        UUID getProgramId();

        UUID getSubjectId();

        Double getCredit();
    }

    /**
     * The credits a programme requires, as its curriculum lists them.
     *
     * <p>Returned per subject rather than summed in SQL: a subject scheduled in
     * two terms is still one requirement, and {@code sum(distinct credit)}
     * would collapse two different 3-credit subjects into one.
     */
    @Query("""
            select distinct c.program.id as programId,
                            c.subject.subjectId as subjectId,
                            c.subject.credit as credit
            from Curriculum c
            where c.isDeleted = false
              and c.program is not null
              and c.subject is not null
            """)
    List<ProgramSubjectCredit> findCurriculumCredits();
}
