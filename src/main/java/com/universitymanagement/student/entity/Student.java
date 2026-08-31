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

    private LocalDate dob;
    private String address;
    private String fatherContact;
    private String motherContact;
    private String gender;

    @Column(name = "status", length = 30)
    private String status = "active";

    @Column(name = "graduation_status")
    private String graduationStatus = "enrolled";

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @OneToMany(mappedBy = "student")
    private List<ClassroomStudent> classroomStudents;

    // No explicit fetch meant EAGER (the JPA default for @ManyToOne) — every
    // load of a Student, including every roster/list of students, silently
    // fired one extra query per row to pull in Program. LAZY here; callers
    // that genuinely need the program in bulk should JOIN FETCH it, the way
    // findRosterWithUser already does for the same reason.
    @ManyToOne(fetch = FetchType.LAZY)
    private Program program;
}