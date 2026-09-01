package com.universitymanagement.department.repository;

import java.util.UUID;

/**
 * How many of something a department holds.
 *
 * <p>Exists so the counts can be fetched for a whole page in one query instead
 * of one per row. A department with none of the thing being counted is simply
 * absent from the result — callers read through a map with a zero default,
 * which is cheaper than an outer join for the same answer.
 */
public record DepartmentCount(UUID departmentId, long total) {
}
