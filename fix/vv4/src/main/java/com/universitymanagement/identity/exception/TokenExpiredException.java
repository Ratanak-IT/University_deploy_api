package com.universitymanagement.identity.exception;

public class TokenExpiredException extends IdentityException {
    public TokenExpiredException() { super("Token expired", 401); }
}
