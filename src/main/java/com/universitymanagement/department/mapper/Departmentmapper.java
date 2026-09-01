package com.universitymanagement.department.mapper;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.entity.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface Departmentmapper {

    /**
     * The counts are parameters rather than fields read off the entity, so the
     * compiler refuses any call site that has not worked out where they come
     * from. The previous single-argument version silently produced zeros, which
     * is exactly how the detail view came to show "0 teachers" for departments
     * that plainly had some.
     */
    DepartmentResponse toResponse(Department department, long teacherCount, long subjectCount);

    Department toEntity(DepartmentRequest departmentRequest);
}
