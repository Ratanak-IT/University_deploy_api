package com.universitymanagement.attendance.entity;

/** Review state of an excused-absence claim. NONE applies to non-excused records. */
public enum ExcuseApprovalStatus {
    NONE,
    PENDING,
    APPROVED,
    REJECTED
}
