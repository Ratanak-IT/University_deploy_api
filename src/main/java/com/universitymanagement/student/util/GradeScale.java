package com.universitymanagement.student.util;

public final class GradeScale {

    private GradeScale() {
    }

    public static String toLetter(double percent) {
        if (percent >= 85) return "A";
        if (percent >= 80) return "B+";
        if (percent >= 70) return "B";
        if (percent >= 65) return "C+";
        if (percent >= 50) return "C";
        if (percent >= 45) return "D";
        if (percent >= 40) return "E";
        return "F";
    }

    public static double toGradePoint(double percent) {
        if (percent >= 85) return 4.0;
        if (percent >= 80) return 3.5;
        if (percent >= 70) return 3.0;
        if (percent >= 65) return 2.5;
        if (percent >= 50) return 2.0;
        if (percent >= 45) return 1.5;
        if (percent >= 40) return 1.0;
        return 0.0;
    }
}
