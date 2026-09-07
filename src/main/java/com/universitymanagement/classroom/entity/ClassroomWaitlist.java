package com.universitymanagement.classroom.entity;

import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "classroom_waitlist",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classroom_waitlist_classroom_student",
                columnNames = {"classroom_id", "student_id"}
        )
)
@Setter
@Getter
@NoArgsConstructor
public class ClassroomWaitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();
}
