package com.universitymanagement.quiz.repository;

import com.universitymanagement.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    @Query("""
            select q from Quiz q
            where q.isDeleted = false
              and q.classroom.classroomId in (
                  select cs.classroom.classroomId
                  from ClassroomStudent cs
                  where cs.student.studentId = :studentId
              )
            order by q.startAt desc
            """)
    List<Quiz> findAllForStudent(@Param("studentId") UUID studentId);
}
