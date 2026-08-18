package com.universitymanagement.teacher.mapper;

import com.universitymanagement.department.entity.Department;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.mapper.SubjectMapper;
import com.universitymanagement.teacher.dto.response.TeacherDepartmentResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.entity.Teacher;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class TeacherMapper {

    @Autowired
    protected MinioService minioService;

    @Autowired
    protected SubjectMapper subjectMapper;

    public abstract TeacherDepartmentResponse toDepartmentResponse(Department department);

    public TeacherResponse toResponse(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        User user = teacher.getUser();

        List<TeacherDepartmentResponse> departments = teacher.getDepartments() == null
                ? List.of()
                : teacher.getDepartments().stream().map(this::toDepartmentResponse).toList();

        List<UUID> departmentIds = teacher.getDepartments() == null
                ? List.of()
                : teacher.getDepartments().stream().map(Department::getDepartmentId).toList();

        List<SubjectResponse> subjects = teacher.getSubjects() == null
                ? List.of()
                : teacher.getSubjects().stream().map(subjectMapper::toResponse).toList();

        // Never let a presigned-URL failure break the whole list request.
        String avatarUrl = null;
        if (user != null && user.getAvatarObjectName() != null) {
            try {
                avatarUrl = minioService.getAssetPreviewUrl(user.getAvatarObjectName());
            } catch (Exception e) {
                avatarUrl = null;
            }
        }

        return new TeacherResponse(
                teacher.getTeacherId(),
                user != null ? user.getId() : null,
                user != null ? user.getKeycloakId() : null,
                teacher.getTeacherCode(),

                user != null ? user.getFullName() : null,
                user != null ? user.resolvedFirstName() : null,
                user != null ? user.resolvedLastName() : null,
                user != null ? user.getNameKhmer() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null,
                user != null && user.getGender() != null ? user.getGender().name() : null,
                user != null ? user.getDateOfBirth() : null,
                user != null ? user.getIdCardNumber() : null,
                user != null ? user.getPlaceOfBirth() : null,
                user != null && user.getCurrentAddress() != null
                        ? user.getCurrentAddress()
                        : (user != null ? user.getAddress() : null),
                user != null ? user.getAddress() : null,
                avatarUrl,

                teacher.getSpecialization(),
                departments,
                departmentIds,
                teacher.getPosition(),
                teacher.getHireDate(),
                teacher.getEmploymentStatus(),
                teacher.getEmploymentStatus(),
                subjects
        );
    }
}