package com.universitymanagement.grading.calc;

import com.universitymanagement.grading.entity.ComponentSource;

import java.util.UUID;

/**
 * How one student did on one component of the grading policy.
 *
 * @param percent null when nothing has been marked yet — which is different
 *                from having scored zero, and must stay different all the way
 *                out to the UI.
 */
public record ComponentResult(
        UUID componentId,
        String name,
        ComponentSource source,
        double weightPercent,
        Double percent,
        double earnedPoints,
        double possiblePoints,
        int gradedItems,
        int totalItems
) {
    public boolean hasData() {
        return percent != null;
    }
}
