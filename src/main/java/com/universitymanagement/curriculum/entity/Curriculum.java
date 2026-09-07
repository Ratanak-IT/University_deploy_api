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

    /**
     * The academic year (e.g. "2025-2026") this requirement took effect. Null means
     * "always applied" (the pre-versioning default). Lets a future curriculum edit
     * be scoped to new cohorts without silently rewriting the plan for students
     * who already enrolled under the prior requirements.
     */
    private String effectiveAcademicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    private Subject subject;
}
