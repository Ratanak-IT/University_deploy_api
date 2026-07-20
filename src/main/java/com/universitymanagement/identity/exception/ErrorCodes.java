package com.universitymanagement.identity.exception;

public final class ErrorCodes {
    private ErrorCodes() {}

    private static final String INVALID_AUTHORIZATION_CODE = "AUTH-001";
    private static final String INVALID_REFRESH_TOKEN = "AUTH-002";
    private static final String KEYCLOAK_USER_NOT_FOUND = "KC-404";
    private static final String KEYCLOAK_ROLE_NOT_FOUND = "KC-405";
    private static final String KEYCLOAK_OPERATION_FAILED = "KC-500";
    public static final String KEYCLOAK_UNAVAILABLE = "KC-503";
    public static final String INTERNAL_SERVER_ERROR = "SYS-500";

}

