package com.universitymanagement.lesson.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lesson_files")
@Getter
@Setter
@NoArgsConstructor
public class LessonFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private String fileObjectName;

    private String fileOriginalName;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}