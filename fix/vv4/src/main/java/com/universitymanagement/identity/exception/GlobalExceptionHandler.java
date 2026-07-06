package com.universitymanagement.identity.exception;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================
    // Validation Exceptions
    // =========================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Validation error: {}", detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildProblemDetail(
                        HttpStatus.BAD_REQUEST,
                        "Validation Failed",
                        detail,
                        "VAL-400",
                        request));
    }

    // =========================
    // Business Logic Exceptions
    // =========================

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        log.warn("Business logic error: {}", ex.getMessage());

        return ResponseEntity.status(ex.getStatusCode())
                .body(buildProblemDetail(
                        HttpStatus.valueOf(ex.getStatusCode().value()),
                        "Business Logic Error",
                        ex.getReason(),
                        "BIZ-400",
                        request));
    }

    // =========================
    // Authentication Exceptions
    // =========================

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Invalid login credentials: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildProblemDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid Credentials",
                        ex.getMessage(),
                        "AUTH-003",
                        request));
    }

    @ExceptionHandler(InvalidAuthorizationCodeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAuthorizationCode(
            InvalidAuthorizationCodeException ex,
            HttpServletRequest request) {

        log.warn("Invalid authorization code: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildProblemDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid Authorization Code",
                        ex.getMessage(),
                        "AUTH-001",
                        request));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex,
            HttpServletRequest request) {

        log.warn("Invalid refresh token: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildProblemDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid Refresh Token",
                        ex.getMessage(),
                        "AUTH-002",
                        request));
    }

    // =========================
    // Keycloak Exceptions
    // =========================

    @ExceptionHandler(KeycloakUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleKeycloakUnavailable(
            KeycloakUnavailableException ex,
            HttpServletRequest request) {

        log.error("Keycloak unavailable", ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildProblemDetail(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Keycloak Unavailable",
                        ex.getMessage(),
                        "KC-503",
                        request));
    }

    @ExceptionHandler(KeycloakUserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(
            KeycloakUserNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Keycloak user not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "User Not Found",
                        ex.getMessage(),
                        "KC-404",
                        request));
    }

    @ExceptionHandler(KeycloakRoleNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleRoleNotFound(
            KeycloakRoleNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Keycloak role not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Role Not Found",
                        ex.getMessage(),
                        "KC-405",
                        request));
    }

    @ExceptionHandler(KeycloakOperationException.class)
    public ResponseEntity<ProblemDetail> handleKeycloakOperation(
            KeycloakOperationException ex,
            HttpServletRequest request) {

        log.error("Keycloak operation failed", ex);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(buildProblemDetail(
                        HttpStatus.BAD_GATEWAY,
                        "Keycloak Operation Failed",
                        ex.getMessage(),
                        "KC-500",
                        request));
    }

    // =========================
    // Generic Exception
    // =========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildProblemDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        "An unexpected error occurred.",
                        "SYS-500",
                        request));
    }


    private ProblemDetail buildProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setTitle(title);
        problem.setDetail(detail);

        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());

        return problem;
    }
}