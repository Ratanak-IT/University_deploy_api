package com.universitymanagement.certificate.entity;

/** Whether an issued certificate still stands. */
public enum IssuedStatus {

    /** Issued and valid. This is what the student can see and download. */
    ISSUED,

    /**
     * Withdrawn — issued in error, or the award was rescinded. The row stays so
     * the verification page can say "this was revoked" rather than "no such
     * certificate", which is a different and much weaker answer.
     */
    REVOKED
}
