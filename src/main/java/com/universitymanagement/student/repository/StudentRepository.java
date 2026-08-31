package com.universitymanagement.student.repository;

import com.universitymanagement.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByUserId(UUID id);
    boolean existsByStudentCode(String studentCode);

    /**
     * Same as {@link #findByUserId}, with the program already loaded — this
     * is what "my profile" reads on every page load, and program is now lazy
     * (it used to default to eager, silently costing a query per student
     * anywhere a list of students was loaded). Without open-in-view keeping a
     * session around, a plain findByUserId here would throw
     * LazyInitializationException the moment the response reads the program
     * name outside the transaction.
     */
    @Query("""
            select s from Student s
            left join fetch s.program
            where s.user.id = :userId
            """)
    Optional<Student> findByUserIdWithProgram(@Param("userId") UUID userId);

    @Query("""
            select s from Student s
            join s.user u
            where lower(s.studentCode) like lower(concat('%', :keyword, '%'))
               or lower(u.fullName)    like lower(concat('%', :keyword, '%'))
               or lower(u.email)       like lower(concat('%', :keyword, '%'))
            """)
    Page<Student> search(@Param("keyword") String keyword, Pageable pageable);
    List<Student> findByProgram_Id(UUID programId);

    /** Enrolled headcount per program. */
    interface ProgramStudentCount {
        UUID getProgramId();

        long getTotal();
    }

    /** Counts every program in one query, so the list screen stays O(1) in queries. */
    @Query("""
            select s.program.id as programId, count(s) as total
            from Student s
            where s.program is not null
            group by s.program.id
            """)
    List<ProgramStudentCount> countStudentsByProgram();

    /**
     * The cohort behind the transcript list, with the user joined so names and
     * photos do not cost a query each.
     */
    @Query("""
            select s from Student s
            left join fetch s.user
            left join fetch s.program
            where (:programId is null or s.program.id = :programId)
              and (:yearLevel is null or s.yearLevel = :yearLevel)
              and (:semester is null or s.semester = :semester)
              and (:academicYear is null or s.academicYear = :academicYear)
            """)
    List<Student> findCohort(@Param("programId") UUID programId,
                             @Param("yearLevel") Integer yearLevel,
                             @Param("semester") Integer semester,
                             @Param("academicYear") String academicYear);
}