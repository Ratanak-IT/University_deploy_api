package com.universitymanagement.teacher.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.universitymanagement.admin.dto.GenderOption;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTeacherRequest(

        String teacherCode,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotBlank
        String confirmPassword,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @JsonAlias({"khmerName", "fullNameKhmer"})
        String nameKhmer,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{8,15}$")
        String phoneNumber,

        @NotNull(message = "Date of birth cannot be null")
        @JsonAlias({"dob", "birthDate"})
        LocalDate dateOfBirth,

        @NotNull(message = "Gender cannot be null")
        GenderOption gender,

        @JsonAlias({"idCard", "nationalId"})
        String idCardNumber,

        @JsonAlias("birthPlace")
        String placeOfBirth,

        String currentAddress,

        String address,

        String specialization,

        List<UUID> departmentIds,

        String position,

        LocalDate hireDate,

        String employmentStatus
) {
}