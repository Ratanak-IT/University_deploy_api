package com.universitymanagement.identity.dto;

import com.universitymanagement.admin.dto.GenderOption;

import java.time.LocalDate;


public interface PersonalInfoPatch {

    String firstName();

    String lastName();

    String fullName();

    String nameKhmer();

    String email();

    GenderOption gender();

    String idCardNumber();

    String placeOfBirth();

    String currentAddress();

    /** Legacy/permanent address field. */
    String address();

    String phoneNumber();

    LocalDate dateOfBirth();

    String fatherContact();

    String motherContact();
}