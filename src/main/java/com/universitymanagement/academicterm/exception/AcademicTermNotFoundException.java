package com.universitymanagement.academicterm.exception;

import java.util.UUID;

public class AcademicTermNotFoundException extends RuntimeException {
    public AcademicTermNotFoundException(UUID termId) {
        super("Academic term not found: " + termId);
    }
}
