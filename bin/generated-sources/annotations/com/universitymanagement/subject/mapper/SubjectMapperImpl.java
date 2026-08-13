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
    date = "2026-08-13T11:15:47+0700",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class SubjectMapperImpl implements SubjectMapper {

    @Override
    public SubjectResponse toResponse(Subject subject) {
        if ( subject == null ) {
            return null;
        }

        UUID departmentId = null;
        UUID subjectId = null;
        String subjectCode = null;
        String subjectName = null;
        Double credit = null;
        Integer hours = null;

        departmentId = subjectDepartmentDepartmentId( subject );
        subjectId = subject.getSubjectId();
        subjectCode = subject.getSubjectCode();
        subjectName = subject.getSubjectName();
        credit = subject.getCredit();
        hours = subject.getHours();

        SubjectResponse subjectResponse = new SubjectResponse( subjectId, subjectCode, subjectName, credit, hours, departmentId );

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
        subject.setHours( subjectRequest.hours() );

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
