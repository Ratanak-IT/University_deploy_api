package com.universitymanagement.student.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentUpdateRequest {

    @Size(max = 20)
    private String academicYear;

    @Min(value = 1, message = "Year level must be at least 1")
    @Max(value = 10, message = "Year level must not exceed 10")
    private Integer yearLevel;

    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 3, message = "Semester must not exceed 3")
    private Integer semester;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @Size(max = 50)
    private String gender;

    @Size(max = 30)
    private String fatherContact;

    @Size(max = 30)
    private String motherContact;

    @Size(max = 500)
    private String address;

    private UUID programId;
}