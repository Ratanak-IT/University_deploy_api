package com.universitymanagement.teacher.mapper;

import com.universitymanagement.department.entity.Department;
import com.universitymanagement.teacher.dto.response.TeacherDepartmentResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.entity.Teacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TeacherMapper {

    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    TeacherResponse toResponse(Teacher teacher);

    TeacherDepartmentResponse toDepartmentResponse(Department department);
}
