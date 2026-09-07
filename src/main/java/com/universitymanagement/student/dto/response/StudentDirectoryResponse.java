package com.universitymanagement.student.dto.response;

import java.util.UUID;

/**
 * The slice of a student's record safe to hand to a teacher picking who to
 * enroll — name, code, and a couple of academic labels for disambiguation.
 * {@link StudentAdminResponse} carries ID card numbers, home address, and
 * parents' contact details, which have no business appearing in an "add a
 * student to my classroom" search.
 */
public record StudentDirectoryResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String email,
        String avatarUrl,
        Integer yearLevel,
        String departmentName
) {
}
