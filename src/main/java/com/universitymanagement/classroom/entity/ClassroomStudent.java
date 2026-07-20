package com.universitymanagement.classroom.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "classroom_students",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_classroom_student",
                        columnNames = {"classroom_id", "student_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ClassroomStudent extends BasedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
}
