package com.universitymanagement.attendance.dto.response;

/**
 * What opening a day's register found.
 *
 * <p>"Nothing to open" is a normal answer, not an error: a Saturday, or a date
 * the class simply does not meet. Saying so explicitly is what stops the screen
 * conjuring a session for every day somebody happens to browse to — those
 * phantom registers then sit in the history looking like classes that were
 * never marked.
 */
public record OpenSessionResponse(
        boolean opened,
        /** Why nothing was opened. Null when {@code opened} is true. */
        String reason,
        /** Null when {@code opened} is false. */
        SessionRegisterResponse register
) {
    public static OpenSessionResponse of(SessionRegisterResponse register) {
        return new OpenSessionResponse(true, null, register);
    }

    public static OpenSessionResponse none(String reason) {
        return new OpenSessionResponse(false, reason, null);
    }
}
