package com.universitymanagement.identity.exception;

public class IdentityException extends RuntimeException {
    private final int statusCode;
    public IdentityException(String msg, int code) {
        super(msg); this.statusCode = code;
    }
    public int getStatusCode() { return statusCode; }
}
