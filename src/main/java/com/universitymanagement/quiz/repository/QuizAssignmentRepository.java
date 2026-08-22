package com.universitymanagement.quiz.repository;

import com.universitymanagement.quiz.entity.QuizAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, UUID> {

    @Query("""
            select a from QuizAssignment a
            join fetch a.classroom c
            left join fetch c.subject
            where a.quiz.quizId = :quizId
            order by c.className asc
            """)
    List<QuizAssignment> findByQuizWithClassroom(UUID quizId);

    List<QuizAssignment> findByQuiz_QuizIdIn(List<UUID> quizIds);

    List<QuizAssignment> findByClassroom_ClassroomId(UUID classroomId);

    List<QuizAssignment> findByClassroom_ClassroomIdIn(List<UUID> classroomIds);

    Optional<QuizAssignment> findByQuiz_QuizIdAndClassroom_ClassroomId(UUID quizId, UUID classroomId);

    boolean existsByQuiz_QuizIdAndClassroom_ClassroomId(UUID quizId, UUID classroomId);

    void deleteByQuiz_QuizId(UUID quizId);

    /** Every release a student can see, through the classrooms they're enrolled in. */
    @Query("""
            select a from QuizAssignment a
            join fetch a.quiz q
            join fetch a.classroom c
            left join fetch c.subject
            where q.isDeleted = false
              and c.isDeleted = false
              and c.classroomId in (
                  select cs.classroom.classroomId
                  from ClassroomStudent cs
                  where cs.student.studentId = :studentId
              )
            """)
    List<QuizAssignment> findAllForStudent(UUID studentId);

    /** Whether this student is enrolled in any classroom the quiz was released to. */
    @Query("""
            select count(a) > 0 from QuizAssignment a
            where a.quiz.quizId = :quizId
              and a.classroom.classroomId in (
                  select cs.classroom.classroomId
                  from ClassroomStudent cs
                  where cs.student.studentId = :studentId
              )
            """)
    boolean isReleasedToStudent(UUID quizId, UUID studentId);

    /** The release a student sits under — the window checks read from it. */
    @Query("""
            select a from QuizAssignment a
            join fetch a.quiz
            join fetch a.classroom
            where a.quiz.quizId = :quizId
              and a.classroom.classroomId in (
                  select cs.classroom.classroomId
                  from ClassroomStudent cs
                  where cs.student.studentId = :studentId
              )
            """)
    List<QuizAssignment> findForStudentAndQuiz(UUID quizId, UUID studentId);
}
