package com.universitymanagement.grading;

import com.universitymanagement.grading.entity.AcademicStanding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcademicStandingTest {

    @Test
    void bandsFollowTheThresholds() {
        assertEquals(AcademicStanding.DEANS_LIST, AcademicStanding.of(4.0));
        assertEquals(AcademicStanding.DEANS_LIST, AcademicStanding.of(3.5));
        assertEquals(AcademicStanding.GOOD_STANDING, AcademicStanding.of(3.49));
        assertEquals(AcademicStanding.GOOD_STANDING, AcademicStanding.of(2.0));
        assertEquals(AcademicStanding.PROBATION, AcademicStanding.of(1.99));
        assertEquals(AcademicStanding.PROBATION, AcademicStanding.of(0.0));
    }

    @Test
    void aStudentWithNothingPostedIsNotOnProbation() {
        assertEquals(AcademicStanding.NO_RECORD, AcademicStanding.of(null),
                "a first-term student has no record, which is not the same as failing");
    }

    @Test
    void orderingPutsTroubleFirstWhenReversed() {
        // The cohort list sorts on ordinal descending, so the two standings a
        // registrar needs to act on have to outrank the ones they do not.
        assertEquals(0, AcademicStanding.DEANS_LIST.ordinal());
        assertEquals(1, AcademicStanding.GOOD_STANDING.ordinal());
        assertEquals(2, AcademicStanding.PROBATION.ordinal());
        assertEquals(3, AcademicStanding.NO_RECORD.ordinal());
    }
}
