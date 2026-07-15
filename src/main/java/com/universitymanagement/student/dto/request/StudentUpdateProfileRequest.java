package com.universitymanagement.student.dto.request;


import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentUpdateProfileRequest {

    private LocalDate dob;

    private String gender;

    private String fatherContact;

    private String motherContact;

    private String address;

    private String phone;

}
