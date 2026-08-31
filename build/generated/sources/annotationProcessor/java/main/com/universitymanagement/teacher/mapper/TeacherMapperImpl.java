package com.universitymanagement.teacher.mapper;

import com.universitymanagement.department.entity.Department;
import com.universitymanagement.teacher.dto.response.TeacherDepartmentResponse;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-26T08:34:32+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class TeacherMapperImpl extends TeacherMapper {

    @Override
    public TeacherDepartmentResponse toDepartmentResponse(Department department) {
        if ( department == null ) {
            return null;
        }

        UUID departmentId = null;
        String departmentName = null;
        String departmentCode = null;

        departmentId = department.getDepartmentId();
        departmentName = department.getDepartmentName();
        departmentCode = department.getDepartmentCode();

        TeacherDepartmentResponse teacherDepartmentResponse = new TeacherDepartmentResponse( departmentId, departmentName, departmentCode );

        return teacherDepartmentResponse;
    }
}
