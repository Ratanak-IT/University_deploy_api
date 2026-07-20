package com.universitymanagement.assignment.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
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
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    private Double maxScore;

    private Double weight;

    // teacher attachments in MinIO
    @OneToMany(
            mappedBy = "assignment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AssignmentFile> files = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_teacher_id")
    private Teacher createdByTeacher;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}