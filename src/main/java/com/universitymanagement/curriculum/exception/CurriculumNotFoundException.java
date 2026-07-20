package com.universitymanagement.curriculum.exception;

import java.util.UUID;

public class CurriculumNotFoundException extends RuntimeException {
    public CurriculumNotFoundException(UUID curriculumId) {
        super("Curriculum not found with id: " + curriculumId);
    }

    public CurriculumNotFoundException(String message) {
        super(message);
    }}
