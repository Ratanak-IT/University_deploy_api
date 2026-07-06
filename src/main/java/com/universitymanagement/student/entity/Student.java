package com.universitymanagement.student.entity;


import com.universitymanagement.identity.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    // IMPORTANT: each student is ONE user
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true)
    private User user;

    @Column(name = "student_code", unique = true, nullable = false)
    private String studentCode;

//    @ManyToOne
//    @JoinColumn(name = "program_id")
//    private Program program;

    private String academicYear;
    private Integer yearLevel;
    private Integer semester;

    private LocalDate dob;
    private String gender;

    private String fatherPhone;
    private String motherPhone;

    @Column(columnDefinition = "TEXT")
    private String address;

    private LocalDate enrollmentDate;

    private String graduationStatus = "enrolled";
    private LocalDate graduationDate;

}
