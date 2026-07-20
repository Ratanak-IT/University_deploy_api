package com.universitymanagement.student.dto.request;


import lombok.*;

import java.time.LocalDate;


public record StudentUpdateProfileRequest(
        String fatherContact,
        String motherContact,
         String address,
        String phone
) {


}
