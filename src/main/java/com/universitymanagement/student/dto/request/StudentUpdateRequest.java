package com.universitymanagement.student.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.universitymanagement.admin.dto.GenderOption;
import com.universitymanagement.identity.dto.PersonalInfoPatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record StudentUpdateRequest(


        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 200)
        String fullName,

        @Size(max = 200)
        @JsonAlias({"khmerName", "fullNameKhmer"})
        String nameKhmer,

        @Email(message = "Email must be valid")
        String email,

        GenderOption gender,

        @Size(max = 50)
        @JsonAlias({"idCard", "nationalId"})
        String idCardNumber,

        @Size(max = 200)
        @JsonAlias("birthPlace")
        String placeOfBirth,

        @Size(max = 500)
        String currentAddress,

        @Size(max = 500)
        String address,

        @Size(max = 30)
        @JsonAlias("phone")
        String phoneNumber,

        @Past(message = "Date of birth must be in the past")
        @JsonAlias({"dob", "birthDate"})
        LocalDate dateOfBirth,

        @Size(max = 30)
        String fatherContact,

        @Size(max = 30)
        String motherContact,


        @Size(max = 50)
        String studentCode,

        @Size(max = 20)
        String academicYear,

        @Min(value = 1, message = "Year level must be at least 1")
        @Max(value = 10, message = "Year level must not exceed 10")
        Integer yearLevel,

        @Min(value = 1, message = "Semester must be at least 1")
        @Max(value = 3, message = "Semester must not exceed 3")
        Integer semester,

        UUID programId,

        LocalDate enrollmentDate,

        @Size(max = 30)
        String status,

        @Size(max = 30)
        String graduationStatus,

        LocalDate graduationDate

) implements PersonalInfoPatch {
}