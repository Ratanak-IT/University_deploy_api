package com.universitymanagement.subject.mapper;

import com.universitymanagement.department.entity.Department;
import com.universitymanagement.subject.dto.request.SubjectRequest;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-16T01:47:57+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class SubjectMapperImpl implements SubjectMapper {

    @Override
    public SubjectResponse toResponse(Subject subject) {
        if ( subject == null ) {
            return null;
        }

        UUID departmentId = null;
        String subjectCode = null;
        String subjectName = null;
        Double credit = null;

        departmentId = subjectDepartmentDepartmentId( subject );
        subjectCode = subject.getSubjectCode();
        subjectName = subject.getSubjectName();
        credit = subject.getCredit();

        SubjectResponse subjectResponse = new SubjectResponse( subjectCode, subjectName, credit, departmentId );

        return subjectResponse;
    }

    @Override
    public Subject toEntity(SubjectRequest subjectRequest) {
        if ( subjectRequest == null ) {
            return null;
        }

        Subject subject = new Subject();

        subject.setSubjectName( subjectRequest.subjectName() );
        subject.setCredit( subjectRequest.credit() );

        return subject;
    }

    private UUID subjectDepartmentDepartmentId(Subject subject) {
        Department department = subject.getDepartment();
        if ( department == null ) {
            return null;
        }
        return department.getDepartmentId();
    }
}
