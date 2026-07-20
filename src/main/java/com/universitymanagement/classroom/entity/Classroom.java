package com.universitymanagement.classroom.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "classrooms")
@Setter
@Getter
@NoArgsConstructor
public class Classroom extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID classroomId;

    @Column(nullable = false)
    private String className;

    @Column(unique = true, nullable = false)
    private String classCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    private String academicYear;

    private Integer semester;

    private Integer yearLevel;

    @Column(unique = true)
    private String inviteCode;

    private String room;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(
            mappedBy = "classroom",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ClassroomStudent> students = new ArrayList<>();

}
