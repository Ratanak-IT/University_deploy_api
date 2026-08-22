package com.universitymanagement.grading.repository;

import com.universitymanagement.grading.entity.GradeComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GradeComponentRepository extends JpaRepository<GradeComponent, UUID> {

    List<GradeComponent> findByClassroom_ClassroomIdOrderByPositionAsc(UUID classroomId);

    boolean existsByClassroom_ClassroomId(UUID classroomId);

    /**
     * Components for a set of classrooms with their assessments already joined —
     * the sheet and transcript views need every offering at once, and fetching
     * them lazily one classroom at a time is what made those pages crawl.
     */
    @Query("""
            select distinct c from GradeComponent c
            left join fetch c.assessments a
            where c.classroom.classroomId in :classroomIds
            order by c.position asc
            """)
    List<GradeComponent> findWithAssessmentsByClassroomIds(List<UUID> classroomIds);
}
