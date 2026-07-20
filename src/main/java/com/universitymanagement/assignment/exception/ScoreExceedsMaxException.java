package com.universitymanagement.assignment.exception;

public class ScoreExceedsMaxException extends RuntimeException {
    public ScoreExceedsMaxException(double maxScore) {
        super("Score cannot exceed max score of " + maxScore);
    }
}
