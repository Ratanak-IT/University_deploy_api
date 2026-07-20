package com.universitymanagement.identity.exception;

public class KeycloakUserNotFoundException extends RuntimeException {
    public KeycloakUserNotFoundException(String message) { super(message); }
}