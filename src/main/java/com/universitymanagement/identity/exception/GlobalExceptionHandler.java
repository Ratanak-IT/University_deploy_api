package com.universitymanagement.identity.exception;

import com.universitymanagement.classroom.exception.*;
import com.universitymanagement.curriculum.exception.CurriculumNotFoundException;
import com.universitymanagement.curriculum.exception.DuplicateCurriculumException;
import com.universitymanagement.department.exception.DepartmentNotFoundException;
import com.universitymanagement.department.exception.DuplicateDepartmentException;
import com.universitymanagement.program.exception.DuplicateProgramException;
import com.universitymanagement.program.exception.ProgramNotFoundException;
import com.universitymanagement.subject.exception.DuplicateSubjectException;
import com.universitymanagement.subject.exception.SubjectNotFoundException;
import com.universitymanagement.student.exception.StudentNotFoundException;
import com.universitymanagement.teacher.exception.TeacherInactiveException;
import com.universitymanagement.teacher.exception.TeacherNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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


    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDepartmentNotFound(
            DepartmentNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Department not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Department Not Found",
                        ex.getMessage(),
                        "DEP-404",
                        request));
    }

    @ExceptionHandler(DuplicateDepartmentException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateDepartment(
            DuplicateDepartmentException ex,
            HttpServletRequest request) {

        log.warn("Duplicate department: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildProblemDetail(
                        HttpStatus.CONFLICT,
                        "Duplicate Department",
                        ex.getMessage(),
                        "DEP-409",
                        request));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAuthorizationDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request) {
        log.warn("Authorization denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildProblemDetail(
                        HttpStatus.FORBIDDEN,
                        "Access Denied",
                        "You do not have permission to perform this action.",
                        "SEC-403",
                        request));
    }
    @ExceptionHandler(ProgramNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProgramNotFound(
            ProgramNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Program not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Program Not Found",
                        ex.getMessage(),
                        "PRG-404",
                        request));
    }

    @ExceptionHandler(DuplicateProgramException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateProgram(
            DuplicateProgramException ex,
            HttpServletRequest request) {

        log.warn("Duplicate program: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildProblemDetail(
                        HttpStatus.CONFLICT,
                        "Duplicate Program",
                        ex.getMessage(),
                        "PRG-409",
                        request));
    }
    @ExceptionHandler(SubjectNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleSubjectNotFound(
            SubjectNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Subject not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Subject Not Found",
                        ex.getMessage(),
                        "SUB-404",
                        request));
    }

    @ExceptionHandler(DuplicateSubjectException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateSubject(
            DuplicateSubjectException ex,
            HttpServletRequest request) {

        log.warn("Duplicate subject: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildProblemDetail(
                        HttpStatus.CONFLICT,
                        "Duplicate Subject",
                        ex.getMessage(),
                        "SUB-409",
                        request));
    }

    @ExceptionHandler(CurriculumNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCurriculumNotFound(
            CurriculumNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Curriculum not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Curriculum Not Found",
                        ex.getMessage(),
                        "CUR-404",
                        request));
    }

    @ExceptionHandler(DuplicateCurriculumException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateCurriculum(
            DuplicateCurriculumException ex,
            HttpServletRequest request) {

        log.warn("Duplicate curriculum: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildProblemDetail(
                        HttpStatus.CONFLICT,
                        "Duplicate Curriculum",
                        ex.getMessage(),
                        "CUR-409",
                        request));
    }

    @ExceptionHandler(TeacherAlreadyAssignedException.class)
    public ResponseEntity<ProblemDetail> handleTeacherAlreadyAssigned(
            TeacherAlreadyAssignedException ex,
            HttpServletRequest request) {

        log.warn("Teacher already assigned: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildProblemDetail(
                        HttpStatus.CONFLICT,
                        "Teacher Already Assigned",
                        ex.getMessage(),
                        "CLS-409-TCH",
                        request));
    }

    @ExceptionHandler(TeacherNotInClassroomException.class)
    public ResponseEntity<ProblemDetail> handleTeacherNotInClassroom(
            TeacherNotInClassroomException ex,
            HttpServletRequest request) {

        log.warn("Teacher not in classroom: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Teacher Not In Classroom",
                        ex.getMessage(),
                        "CLS-404-TCH",
                        request));
    }

    @ExceptionHandler(ClassroomNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClassroomNotFound(
            ClassroomNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Classroom not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Classroom Not Found",
                        ex.getMessage(),
                        "CLS-404",
                        request));
    }

    @ExceptionHandler(StudentNotEnrolledException.class)
    public ResponseEntity<ProblemDetail> handleStudentNotEnrolled(
            StudentNotEnrolledException ex,
            HttpServletRequest request) {

        log.warn("Student not enrolled: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Student Not Enrolled",
                        ex.getMessage(),
                        "CLS-404-ENR",
                        request));
    }

    @ExceptionHandler(ClassroomAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleClassroomAccessDenied(
            ClassroomAccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Classroom access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildProblemDetail(
                        HttpStatus.FORBIDDEN,
                        "Classroom Access Denied",
                        ex.getMessage(),
                        "CLS-403",
                        request));
    }

    @ExceptionHandler(TeacherNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleTeacherNotFound(
            TeacherNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Teacher not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Teacher Not Found",
                        ex.getMessage(),
                        "TCH-404",
                        request));
    }

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleStudentNotFound(
            StudentNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Student not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Student Not Found",
                        ex.getMessage(),
                        "STU-404",
                        request));
    }

    @ExceptionHandler(IdentityException.class)
    public ResponseEntity<ProblemDetail> handleIdentityException(
            IdentityException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.warn("Identity error: {}", ex.getMessage());

        return ResponseEntity.status(status)
                .body(buildProblemDetail(
                        status,
                        status.getReasonPhrase(),
                        ex.getMessage(),
                        "IDN-" + status.value(),
                        request));
    }

    @ExceptionHandler(TeacherInactiveException.class)
    public ResponseEntity<ProblemDetail> handleTeacherInactive(
            TeacherInactiveException ex,
            HttpServletRequest request) {

        log.warn("Teacher inactive: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildProblemDetail(
                        HttpStatus.CONFLICT,
                        "Teacher Inactive",
                        ex.getMessage(),
                        "TCH-409",
                        request));
    }
}