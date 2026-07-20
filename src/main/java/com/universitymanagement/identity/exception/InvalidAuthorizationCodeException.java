package com.universitymanagement.identity.exception;

public class InvalidAuthorizationCodeException extends RuntimeException{

    public  InvalidAuthorizationCodeException(String message) {
        super(message);
    }

    public  InvalidAuthorizationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
