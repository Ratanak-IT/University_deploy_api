package com.universitymanagement.certificate.entity;

/** Lifecycle of a certificate template. */
public enum TemplateStatus {

    /** Being edited. Never used to issue anything. */
    DRAFT,

    /** The one that issuing uses. At most one per type and programme. */
    ACTIVE,

    /**
     * Superseded. Kept rather than deleted because certificates already issued
     * point at the version that produced them, and that trail has to survive.
     */
    ARCHIVED
}
