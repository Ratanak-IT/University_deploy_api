package com.universitymanagement.certificate.service;

import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.grading.entity.AcademicStanding;
import com.universitymanagement.student.dto.response.StudentAcademicSummaryResponse;
import org.springframework.stereotype.Component;

/**
 * Whether a student has earned a given certificate.
 *
 * <p>Lives on its own because two callers need the identical answer: the batch
 * that skips ineligible students, and the request inbox that has to say why a
 * pending request cannot be approved before the registrar clicks. Two copies of
 * this rule would drift, and the drift would show up as a certificate awarded
 * one way but refused the other.
 */
@Component
public class CertificateEligibility {

    /** @return why this student should not receive it, or null if they should */
    public String problemFor(CertificateType type, StudentAcademicSummaryResponse summary) {
        if (summary == null) {
            return "No academic record";
        }

        return switch (type) {
            case DEGREE, COMPLETION -> {
                if (summary.standing() == AcademicStanding.NO_RECORD) {
                    yield "No posted grades yet";
                }
                if (summary.standing() == AcademicStanding.PROBATION) {
                    yield "On academic probation (CGPA %.2f)".formatted(
                            summary.cumulativeGpa() != null ? summary.cumulativeGpa() : 0.0);
                }
                if (!summary.eligibleToGraduate()) {
                    yield "Credits incomplete (%s of %s)".formatted(
                            summary.creditsEarned() != null
                                    ? "%.0f".formatted(summary.creditsEarned()) : "0",
                            summary.creditsRequired() != null
                                    ? "%.0f".formatted(summary.creditsRequired()) : "?");
                }
                yield null;
            }
            case TRANSCRIPT -> summary.coursesPosted() == 0 ? "No posted grades yet" : null;
            // Enrolment confirmation only states that the student is on the
            // books, so there is nothing to have earned.
            case ENROLLMENT_CONFIRMATION -> null;
        };
    }
}
