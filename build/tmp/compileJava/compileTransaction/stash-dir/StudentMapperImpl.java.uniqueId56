package com.universitymanagement.student.mapper;

import com.universitymanagement.identity.entity.User;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.student.dto.response.StudentAdminResponse;
import com.universitymanagement.student.dto.response.StudentResponse;
import com.universitymanagement.student.entity.Student;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-16T01:47:57+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class StudentMapperImpl implements StudentMapper {

    @Override
    public StudentResponse toResponse(Student student) {
        if ( student == null ) {
            return null;
        }

        UUID id = null;
        String email = null;
        String department = null;
        String studentCode = null;

        id = student.getStudentId();
        email = studentUserEmail( student );
        department = studentProgramProgramName( student );
        studentCode = student.getStudentCode();

        StudentResponse studentResponse = new StudentResponse( id, studentCode, email, department );

        return studentResponse;
    }

    @Override
    public StudentAdminResponse toAdminResponse(Student student) {
        if ( student == null ) {
            return null;
        }

        UUID userId = null;
        String keycloakId = null;
        String fullName = null;
        String email = null;
        String phoneNumber = null;
        LocalDate dateOfBirth = null;
        String programName = null;
        UUID studentId = null;
        String studentCode = null;
        String academicYear = null;
        Integer yearLevel = null;
        Integer semester = null;
        LocalDate enrollmentDate = null;
        String graduationStatus = null;
        LocalDate graduationDate = null;

        userId = studentUserId( student );
        keycloakId = studentUserKeycloakId( student );
        fullName = studentUserFullName( student );
        email = studentUserEmail( student );
        phoneNumber = studentUserPhoneNumber( student );
        dateOfBirth = studentUserDateOfBirth( student );
        programName = studentProgramProgramName( student );
        studentId = student.getStudentId();
        studentCode = student.getStudentCode();
        academicYear = student.getAcademicYear();
        yearLevel = student.getYearLevel();
        semester = student.getSemester();
        enrollmentDate = student.getEnrollmentDate();
        graduationStatus = student.getGraduationStatus();
        graduationDate = student.getGraduationDate();

        String gender = student.getUser() != null && student.getUser().getGender() != null ? student.getUser().getGender().name() : null;

        StudentAdminResponse studentAdminResponse = new StudentAdminResponse( studentId, userId, keycloakId, studentCode, fullName, email, phoneNumber, gender, dateOfBirth, academicYear, yearLevel, semester, programName, enrollmentDate, graduationStatus, graduationDate );

        return studentAdminResponse;
    }

    private String studentUserEmail(Student student) {
        User user = student.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getEmail();
    }

    private String studentProgramProgramName(Student student) {
        Program program = student.getProgram();
        if ( program == null ) {
            return null;
        }
        return program.getProgramName();
    }

    private UUID studentUserId(Student student) {
        User user = student.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }

    private String studentUserKeycloakId(Student student) {
        User user = student.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getKeycloakId();
    }

    private String studentUserFullName(Student student) {
        User user = student.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getFullName();
    }

    private String studentUserPhoneNumber(Student student) {
        User user = student.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getPhoneNumber();
    }

    private LocalDate studentUserDateOfBirth(Student student) {
        User user = student.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getDateOfBirth();
    }
}
