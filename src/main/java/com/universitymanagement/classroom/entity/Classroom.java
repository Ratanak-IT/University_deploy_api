package com.universitymanagement.classroom.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.security.auth.Subject;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "classrooms")
@Setter
@Getter
@NoArgsConstructor
public class Classroom extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "classroom_id", nullable = false, updatable = false)
    private UUID classroomId;

    @Column(name = "class_name", nullable = false, length = 150)
    private String className;

    @Column(name = "class_code", nullable = false, unique = true, length = 50)
    private String classCode;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "subject_id", nullable = false)
//    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "year_level", nullable = false)
    private Integer yearLevel;

    @Column(name = "invite_code", length = 20)
    private String inviteCode;

    @Column(name = "room", length = 50)
    private String room;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

}
