package com.universitymanagement.certificate.entity;

/** How a template turns into a certificate. */
public enum RenderMode {

    /** Body written as HTML in the editor. */
    HTML,

    /**
     * Values placed onto an uploaded PDF or image design — the "bring your own
     * artwork" path, where the design is preserved exactly as approved.
     */
    OVERLAY
}
