package com.universitymanagement.student.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class StudentSearchRequest {

    private String keyword;

    private String studentCode;

    private UUID programId;

    private String academicYear;

    private Integer yearLevel;

    private Integer semester;

    private GraduationStatus graduationStatus;
}