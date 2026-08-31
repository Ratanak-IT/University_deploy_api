package com.universitymanagement.assignment.repository;

import com.universitymanagement.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByClassroom_ClassroomIdAndIsDeletedFalseOrderByDueDateAsc(UUID classroomId);
    List<Assignment> findByClassroomIsNullAndCreatedByAndIsDeletedFalseOrderByCreatedAtDesc(String createdBy);

    /**
     * Same as {@link #findByClassroom_ClassroomIdAndIsDeletedFalseOrderByDueDateAsc},
     * with files already loaded — the teacher's classroom assignment list
     * reads every assignment's files to build a preview URL, which cost one
     * extra round trip per assignment against a remote database.
     */
    @Query("""
            select distinct a from Assignment a
            left join fetch a.files
            where a.classroom.classroomId = :classroomId
            and a.isDeleted = false
            order by a.dueDate asc
            """)
    List<Assignment> findByClassroomIdWithFiles(@Param("classroomId") UUID classroomId);

    /**
     * Every assignment across a set of classrooms, in one query, with its
     * files already loaded. A student enrolled in six classes previously
     * meant six separate queries here (one per classroom) plus one more per
     * assignment just to read its files — this is the batched replacement
     * for both.
     */
    @Query("""
            select distinct a from Assignment a
            left join fetch a.files
            left join fetch a.classroom c
            left join fetch c.subject
            where a.classroom.classroomId in :classroomIds
            and a.isDeleted = false
            order by a.dueDate asc
            """)
    List<Assignment> findByClassroomIdsWithFiles(@Param("classroomIds") List<UUID> classroomIds);

    /** Assignment count across a set of classrooms, for the teacher dashboard's "course materials" stat. */
    @Query("""
            select count(a) from Assignment a
            where a.classroom.classroomId in :classroomIds
              and a.isDeleted = false
            """)
    long countByClassroomIds(@Param("classroomIds") List<UUID> classroomIds);

    /**
     * A student's not-yet-submitted assignments, soonest due first — with the
     * classroom already loaded and files/description left untouched. The
     * student dashboard only needs a title, a due date and a class label for
     * its deadlines widget; the general-purpose
     * {@link com.universitymanagement.student.dto.response.StudentAssignmentResponse}
     * path signs a preview URL for every file on every assignment just to
     * have that data thrown away unread.
     */
    @Query("""
            select a from Assignment a
            join fetch a.classroom c
            where a.classroom.classroomId in :classroomIds
              and a.isDeleted = false
              and a.dueDate is not null
              and not exists (
                  select 1 from Submission s
                  where s.assignment = a and s.student.studentId = :studentId
              )
            order by a.dueDate asc
            """)
    List<Assignment> findPendingWithClassroomByClassroomIdsAndStudent(
            @Param("classroomIds") List<UUID> classroomIds,
            @Param("studentId") UUID studentId);

    /**
     * Every assignment across a set of classrooms, classroom and subject
     * loaded — but not files, unlike {@link #findByClassroomIdsWithFiles}.
     * The student assignments list page never renders a file, so fetching
     * one is pure waste: every file signs a fresh MinIO URL.
     */
    @Query("""
            select distinct a from Assignment a
            join fetch a.classroom c
            left join fetch c.subject
            where a.classroom.classroomId in :classroomIds
              and a.isDeleted = false
            order by a.dueDate asc
            """)
    List<Assignment> findByClassroomIdsWithClassroomOnly(@Param("classroomIds") List<UUID> classroomIds);
}
