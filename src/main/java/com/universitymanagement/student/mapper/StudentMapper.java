package com.universitymanagement.student.mapper;

import com.universitymanagement.student.dto.response.StudentAdminResponse;
import com.universitymanagement.student.dto.response.StudentResponse;
import com.universitymanagement.student.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mapping(target = "id", source = "studentId")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "department", source = "program.programName")
    StudentResponse toResponse(Student student);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "keycloakId", source = "user.keycloakId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "gender", expression = "java(student.getUser() != null && student.getUser().getGender() != null ? student.getUser().getGender().name() : null)")
    @Mapping(target = "dateOfBirth", source = "user.dateOfBirth")
    @Mapping(target = "programName", source = "program.programName")
    StudentAdminResponse toAdminResponse(Student student);
}
