package com.universitymanagement.curriculum.entity;

import com.universitymanagement.program.entity.Program;
import com.universitymanagement.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "curriculums")
@Getter
@Setter
@NoArgsConstructor
public class Curriculum {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID curriculumId;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false)
    private Integer yearLevel;

    @Enumerated(EnumType.STRING)
    private CourseType courseType = CourseType.CORE_REQUIRED;

    private UUID prerequisiteSubjectId;

    private Integer lectureHours;

    private Integer labHours;

    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    private Subject subject;
}
