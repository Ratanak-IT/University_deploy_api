package com.universitymanagement.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class StudentCreateRequest {

    @NotNull(message = "User is required")
    private UUID userId;

    @NotBlank(message = "Student code is required")
    private String studentCode;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotNull(message = "Year level is required")
    private Integer yearLevel;

    @NotNull(message = "Semester is required")
    private Integer semester;

    @NotNull(message = "Program is required")
    private UUID programId;

    private LocalDate dob;
    private String gender;
    private String fatherContact;
    private String motherContact;
    private String address;

    @NotNull(message = "Enrollment date is required")
    private LocalDate enrollmentDate;
}