package com.universitymanagement.teacher.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.universitymanagement.admin.dto.GenderOption;
import com.universitymanagement.identity.dto.PersonalInfoPatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateTeacherRequest(


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
        String teacherCode,

        String specialization,

        List<UUID> departmentIds,

        String position,

        LocalDate hireDate,

        String employmentStatus

) implements PersonalInfoPatch {
}