package com.universitymanagement.teacher.mapper;

import com.universitymanagement.department.entity.Department;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.teacher.dto.response.TeacherDepartmentResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.entity.Teacher;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-16T00:52:47+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class TeacherMapperImpl implements TeacherMapper {

    @Override
    public TeacherResponse toResponse(Teacher teacher) {
        if ( teacher == null ) {
            return null;
        }

        String fullName = null;
        String email = null;
        String phoneNumber = null;
        UUID teacherId = null;
        String teacherCode = null;
        String specialization = null;
        List<TeacherDepartmentResponse> departments = null;
        String position = null;
        LocalDate hireDate = null;
        String employmentStatus = null;
        List<SubjectResponse> subjects = null;

        fullName = teacherUserFullName( teacher );
        email = teacherUserEmail( teacher );
        phoneNumber = teacherUserPhoneNumber( teacher );
        teacherId = teacher.getTeacherId();
        teacherCode = teacher.getTeacherCode();
        specialization = teacher.getSpecialization();
        departments = departmentSetToTeacherDepartmentResponseList( teacher.getDepartments() );
        position = teacher.getPosition();
        hireDate = teacher.getHireDate();
        employmentStatus = teacher.getEmploymentStatus();
        subjects = subjectSetToSubjectResponseList( teacher.getSubjects() );

        TeacherResponse teacherResponse = new TeacherResponse( teacherId, teacherCode, fullName, email, phoneNumber, specialization, departments, position, hireDate, employmentStatus, subjects );

        return teacherResponse;
    }

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

    private String teacherUserFullName(Teacher teacher) {
        User user = teacher.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getFullName();
    }

    private String teacherUserEmail(Teacher teacher) {
        User user = teacher.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getEmail();
    }

    private String teacherUserPhoneNumber(Teacher teacher) {
        User user = teacher.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getPhoneNumber();
    }

    protected List<TeacherDepartmentResponse> departmentSetToTeacherDepartmentResponseList(Set<Department> set) {
        if ( set == null ) {
            return null;
        }

        List<TeacherDepartmentResponse> list = new ArrayList<TeacherDepartmentResponse>( set.size() );
        for ( Department department : set ) {
            list.add( toDepartmentResponse( department ) );
        }

        return list;
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

    protected List<SubjectResponse> subjectSetToSubjectResponseList(Set<Subject> set) {
        if ( set == null ) {
            return null;
        }

        List<SubjectResponse> list = new ArrayList<SubjectResponse>( set.size() );
        for ( Subject subject : set ) {
            list.add( subjectToSubjectResponse( subject ) );
        }

        return list;
    }
}
