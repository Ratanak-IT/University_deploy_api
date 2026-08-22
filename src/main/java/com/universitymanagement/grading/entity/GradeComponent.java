package com.universitymanagement.grading.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One line of a course's grading policy, as published in its syllabus —
 * "Midterm 30%", "Final 40%". The weights of a classroom's components must
 * total 100, which is what makes a course grade meaningful rather than an
 * average of whatever happened to be entered.
 */
@Entity
@Table(
        name = "grade_components",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grade_component_classroom_name",
                columnNames = {"classroom_id", "name"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class GradeComponent extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID componentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentSource source = ComponentSource.MANUAL;

    /** Share of the final grade, 0–100. All components of a classroom sum to 100. */
    @Column(name = "weight_percent", nullable = false)
    private Double weightPercent;

    /** Display order in the gradebook. */
    @Column(nullable = false)
    private Integer position = 0;

    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assessment> assessments = new ArrayList<>();
}
