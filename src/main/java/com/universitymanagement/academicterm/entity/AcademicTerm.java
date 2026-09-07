package com.universitymanagement.academicterm.entity;

import com.universitymanagement.auditing.BasedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "academic_terms")
@Setter
@Getter
@NoArgsConstructor
public class AcademicTerm extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID termId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String academicYear;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate addDropDeadline;

    @Column(nullable = false)
    private Boolean isActive = true;
}
