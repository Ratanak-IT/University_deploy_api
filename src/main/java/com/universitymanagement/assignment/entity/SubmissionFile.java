package com.universitymanagement.assignment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "submission_files")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(nullable = false)
    private String fileObjectName;

    private String fileOriginalName;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}