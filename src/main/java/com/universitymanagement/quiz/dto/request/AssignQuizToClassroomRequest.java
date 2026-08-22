package com.universitymanagement.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Releases a quiz to a set of classrooms.
 *
 * <p>The list replaces the quiz's current releases outright, so removing a
 * section is the same call as adding one. The single {@code classroomId} form
 * is still accepted for clients written against the old one-section API.
 */
public record AssignQuizToClassroomRequest(
        /** Legacy single-classroom form. Prefer {@code classrooms}. */
        UUID classroomId,

        List<@Valid ClassroomRelease> classrooms
) {
    public record ClassroomRelease(
            @NotNull(message = "classroomId is required")
            UUID classroomId,

            /** Null falls back to the quiz's own window. */
            LocalDateTime availableFrom,
            LocalDateTime availableTo
    ) {
    }

    /** Normalises both shapes into the one list the service works with. */
    public List<ClassroomRelease> releases() {
        if (classrooms != null && !classrooms.isEmpty()) {
            return classrooms;
        }
        if (classroomId != null) {
            return List.of(new ClassroomRelease(classroomId, null, null));
        }
        return List.of();
    }
}
