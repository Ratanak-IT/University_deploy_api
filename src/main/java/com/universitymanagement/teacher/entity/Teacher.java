package com.universitymanagement.teacher.entity;


import com.universitymanagement.identity.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@Entity
@Table(name = "teachers")
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID teacherId;

    // IMPORTANT: each teacher is ONE user
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true)
    private User user;

    @Column(name = "teacher_code", unique = true, nullable = false)
    private String teacherCode;

    private String department;
    private String position;
    private String specialization;

    private LocalDate hireDate;

    private String employmentStatus = "active";

    private LocalDateTime createdAt;

    public Teacher() {
        this.createdAt = LocalDateTime.now();
    }
}
