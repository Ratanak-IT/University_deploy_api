package com.universitymanagement.department.mapper;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-10T20:49:58+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class DepartmentmapperImpl implements Departmentmapper {

    @Override
    public DepartmentResponse toResponse(Department department) {
        if ( department == null ) {
            return null;
        }

        UUID departmentId = null;
        String departmentName = null;
        List<SubjectResponse> subjects = null;

        departmentId = department.getDepartmentId();
        departmentName = department.getDepartmentName();
        subjects = subjectListToSubjectResponseList( department.getSubjects() );

        DepartmentResponse departmentResponse = new DepartmentResponse( departmentId, departmentName, subjects );

        return departmentResponse;
    }

    @Override
    public Department toEntity(DepartmentRequest departmentRequest) {
        if ( departmentRequest == null ) {
            return null;
        }

        Department department = new Department();

        department.setDepartmentName( departmentRequest.departmentName() );

        return department;
    }

    protected SubjectResponse subjectToSubjectResponse(Subject subject) {
        if ( subject == null ) {
            return null;
        }

        String subjectCode = null;
        String subjectName = null;
        Double credit = null;

        subjectCode = subject.getSubjectCode();
        subjectName = subject.getSubjectName();
        credit = subject.getCredit();

        UUID departmentId = null;

        SubjectResponse subjectResponse = new SubjectResponse( subjectCode, subjectName, credit, departmentId );

        return subjectResponse;
    }

    protected List<SubjectResponse> subjectListToSubjectResponseList(List<Subject> list) {
        if ( list == null ) {
            return null;
        }

        List<SubjectResponse> list1 = new ArrayList<SubjectResponse>( list.size() );
        for ( Subject subject : list ) {
            list1.add( subjectToSubjectResponse( subject ) );
        }

        return list1;
    }
}
