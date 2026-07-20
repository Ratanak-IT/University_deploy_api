package com.universitymanagement.assignment.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignment_student",
                columnNames = {"assignment_id", "student_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Submission extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // student submission files in MinIO
    @OneToMany(
            mappedBy = "submission",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SubmissionFile> files = new ArrayList<>();

    private LocalDateTime submittedAt;

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_teacher_id")
    private Teacher gradedBy;

    private LocalDateTime gradedAt;
}