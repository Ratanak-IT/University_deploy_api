package com.universitymanagement.student.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record TransferCreditResponse(
        UUID transferCreditId,
        UUID studentId,
        UUID subjectId,
        String subjectName,
        String sourceInstitution,
        Double credits,
        LocalDate grantedDate
) {
}
