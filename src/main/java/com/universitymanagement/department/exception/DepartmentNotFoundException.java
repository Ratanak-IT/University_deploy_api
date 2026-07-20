package com.universitymanagement.department.exception;

import java.util.UUID;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(UUID departmentId) {
        super("Department not found with id: " + departmentId);
    }
}
