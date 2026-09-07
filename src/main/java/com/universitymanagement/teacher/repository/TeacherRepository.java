package com.universitymanagement.teacher.repository;

import com.universitymanagement.teacher.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    Optional<Teacher> findByUserId(UUID id);
    boolean existsByTeacherCode(String teacherCode);
    List<Teacher> findByDepartments_DepartmentId(UUID departmentId);

    /** The admin list. Retired teachers are hidden, not gone. */
    @Query("""
            select t from Teacher t
            where t.isDeleted is null or t.isDeleted = false
            """)
    Page<Teacher> findAllLive(Pageable pageable);

    /** Retired teachers, so a mistaken removal can be found and undone. */
    @Query("""
            select t from Teacher t
            where t.isDeleted = true
            """)
    Page<Teacher> findAllWithdrawn(Pageable pageable);

    /**
     * Same as {@link #findByUserId}, with departments already loaded — "my
     * profile" reads them on every load, and without a transaction around
     * that read (there wasn't one), touching the lazy collection threw
     * LazyInitializationException the moment open-in-view stopped covering
     * for it. This is what made GET /teachers/me return 500.
     */
    @Query("""
            select distinct t from Teacher t
            left join fetch t.departments
            where t.user.id = :userId
            """)
    Optional<Teacher> findByUserIdWithDepartments(@Param("userId") UUID userId);
}
