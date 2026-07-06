package com.universitymanagement.identity.exception;

public class UserNotFoundException extends IdentityException {
    public UserNotFoundException() { super("User not found", 404); }
}
