package com.universitymanagement.program.exception;

import java.util.UUID;

public class ProgramNotFoundException extends RuntimeException {
    public ProgramNotFoundException(UUID programId) {
        super("Program not found with id: " + programId);
    }

    public ProgramNotFoundException(String message) {
        super(message);
    }
}
