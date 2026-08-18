package com.universitymanagement.student.mapper;

import com.universitymanagement.identity.entity.User;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.student.dto.response.StudentAdminResponse;
import com.universitymanagement.student.dto.response.StudentResponse;
import com.universitymanagement.student.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class StudentMapper {

    @Autowired
    protected MinioService minioService;

    @Mapping(target = "id", source = "studentId")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "department", source = "program.programName")
    public abstract StudentResponse toResponse(Student student);

    public StudentAdminResponse toAdminResponse(Student student) {
        if (student == null) {
            return null;
        }

        User user = student.getUser();
        Program program = student.getProgram();

        String avatarUrl = null;
        if (user != null && user.getAvatarObjectName() != null) {
            try {
                avatarUrl = minioService.getAssetPreviewUrl(user.getAvatarObjectName());
            } catch (Exception e) {
                avatarUrl = null;
            }
        }

        return new StudentAdminResponse(
                student.getStudentId(),
                user != null ? user.getId() : null,
                user != null ? user.getKeycloakId() : null,
                student.getStudentCode(),

                user != null ? user.getFullName() : null,
                user != null ? user.resolvedFirstName() : null,
                user != null ? user.resolvedLastName() : null,
                user != null ? user.getNameKhmer() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null,
                user != null && user.getGender() != null ? user.getGender().name() : student.getGender(),
                user != null && user.getDateOfBirth() != null ? user.getDateOfBirth() : student.getDob(),
                user != null ? user.getIdCardNumber() : null,
                user != null ? user.getPlaceOfBirth() : null,
                user != null && user.getCurrentAddress() != null
                        ? user.getCurrentAddress()
                        : (user != null ? user.getAddress() : student.getAddress()),
                user != null ? user.getAddress() : student.getAddress(),
                user != null ? user.getFatherContact() : student.getFatherContact(),
                user != null ? user.getMotherContact() : student.getMotherContact(),
                avatarUrl,

                student.getAcademicYear(),
                student.getYearLevel(),
                student.getSemester(),
                program != null ? program.getId() : null,
                program != null ? program.getProgramName() : null,
                program != null && program.getDepartment() != null
                        ? program.getDepartment().getDepartmentId() : null,
                program != null && program.getDepartment() != null
                        ? program.getDepartment().getDepartmentName() : null,
                null,
                student.getEnrollmentDate(),
                student.getStatus() != null ? student.getStatus() : "active",
                student.getGraduationStatus(),
                student.getGraduationDate()
        );
    }
}