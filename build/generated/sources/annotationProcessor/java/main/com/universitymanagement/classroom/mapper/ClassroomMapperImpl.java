package com.universitymanagement.classroom.mapper;

import com.universitymanagement.classroom.dto.request.ClassroomCreateRequest;
import com.universitymanagement.classroom.dto.request.ClassroomUpdateRequest;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.dto.response.ClassroomStudentResponse;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.teacher.entity.Teacher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-15T08:40:39+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class ClassroomMapperImpl implements ClassroomMapper {

    @Override
    public ClassroomResponse toResponse(Classroom classroom) {
        if ( classroom == null ) {
            return null;
        }

        UUID teacherId = null;
        String teacherName = null;
        UUID subjectId = null;
        String subjectName = null;
        UUID programId = null;
        String programName = null;
        LocalDateTime updatedAt = null;
        String updatedBy = null;
        UUID classroomId = null;
        String className = null;
        String classCode = null;
        String academicYear = null;
        Integer semester = null;
        Integer yearLevel = null;
        String inviteCode = null;
        String room = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        LocalDateTime createdAt = null;
        String createdBy = null;

        teacherId = classroomTeacherTeacherId( classroom );
        teacherName = classroomTeacherUserFullName( classroom );
        subjectId = classroomSubjectSubjectId( classroom );
        subjectName = classroomSubjectSubjectName( classroom );
        programId = classroomProgramId( classroom );
        programName = classroomProgramProgramName( classroom );
        updatedAt = classroom.getLastUpdateAt();
        updatedBy = classroom.getLastUpdatedBy();
        classroomId = classroom.getClassroomId();
        className = classroom.getClassName();
        classCode = classroom.getClassCode();
        academicYear = classroom.getAcademicYear();
        semester = classroom.getSemester();
        yearLevel = classroom.getYearLevel();
        inviteCode = classroom.getInviteCode();
        room = classroom.getRoom();
        startDate = classroom.getStartDate();
        endDate = classroom.getEndDate();
        createdAt = classroom.getCreatedAt();
        createdBy = classroom.getCreatedBy();

        ClassroomResponse classroomResponse = new ClassroomResponse( classroomId, className, classCode, teacherId, teacherName, subjectId, subjectName, programId, programName, academicYear, semester, yearLevel, inviteCode, room, startDate, endDate, createdAt, updatedAt, createdBy, updatedBy );

        return classroomResponse;
    }

    @Override
    public Classroom toEntity(ClassroomCreateRequest classroomCreateRequest) {
        if ( classroomCreateRequest == null ) {
            return null;
        }

        Classroom classroom = new Classroom();

        classroom.setClassName( classroomCreateRequest.className() );
        classroom.setAcademicYear( classroomCreateRequest.academicYear() );
        classroom.setSemester( classroomCreateRequest.semester() );
        classroom.setYearLevel( classroomCreateRequest.yearLevel() );
        classroom.setRoom( classroomCreateRequest.room() );
        classroom.setStartDate( classroomCreateRequest.startDate() );
        classroom.setEndDate( classroomCreateRequest.endDate() );

        return classroom;
    }

    @Override
    public ClassroomStudentResponse toStudentResponse(ClassroomStudent classroomStudent) {
        if ( classroomStudent == null ) {
            return null;
        }

        UUID studentId = null;
        String studentCode = null;
        String fullName = null;
        String email = null;
        Integer yearLevel = null;
        Integer semester = null;
        LocalDateTime joinedAt = null;

        ClassroomStudentResponse classroomStudentResponse = new ClassroomStudentResponse( studentId, studentCode, fullName, email, yearLevel, semester, joinedAt );

        return classroomStudentResponse;
    }

    @Override
    public void updateEntity(ClassroomUpdateRequest request, Classroom classroom) {
        if ( request == null ) {
            return;
        }

        classroom.setClassName( request.className() );
        classroom.setAcademicYear( request.academicYear() );
        classroom.setSemester( request.semester() );
        classroom.setYearLevel( request.yearLevel() );
        classroom.setRoom( request.room() );
        classroom.setStartDate( request.startDate() );
        classroom.setEndDate( request.endDate() );
    }

    private UUID classroomTeacherTeacherId(Classroom classroom) {
        Teacher teacher = classroom.getTeacher();
        if ( teacher == null ) {
            return null;
        }
        return teacher.getTeacherId();
    }

    private String classroomTeacherUserFullName(Classroom classroom) {
        Teacher teacher = classroom.getTeacher();
        if ( teacher == null ) {
            return null;
        }
        User user = teacher.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getFullName();
    }

    private UUID classroomSubjectSubjectId(Classroom classroom) {
        Subject subject = classroom.getSubject();
        if ( subject == null ) {
            return null;
        }
        return subject.getSubjectId();
    }

    private String classroomSubjectSubjectName(Classroom classroom) {
        Subject subject = classroom.getSubject();
        if ( subject == null ) {
            return null;
        }
        return subject.getSubjectName();
    }

    private UUID classroomProgramId(Classroom classroom) {
        Program program = classroom.getProgram();
        if ( program == null ) {
            return null;
        }
        return program.getId();
    }

    private String classroomProgramProgramName(Classroom classroom) {
        Program program = classroom.getProgram();
        if ( program == null ) {
            return null;
        }
        return program.getProgramName();
    }
}
