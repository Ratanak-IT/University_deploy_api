package com.universitymanagement.identity.exception;

public class KeycloakRoleNotFoundException extends RuntimeException {
    public KeycloakRoleNotFoundException(String message) { super(message); }
}