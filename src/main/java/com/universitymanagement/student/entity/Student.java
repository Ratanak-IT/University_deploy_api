package com.universitymanagement.student.entity;


import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.program.entity.Program;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "student_id")
    private UUID studentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true)
    private User user;

    @Column(name = "student_code", unique = true, nullable = false)
    private String studentCode;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "year_level")
    private Integer yearLevel;

    private Integer semester;


    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "graduation_status")
    private String graduationStatus = "enrolled";

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @OneToMany(mappedBy = "student")
    private List<ClassroomStudent> classroomStudents;

    @ManyToOne
    private Program program;

}
