package com.universitymanagement.curriculum.mapper;

import com.universitymanagement.curriculum.dto.request.CurriculumRequest;
import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import com.universitymanagement.curriculum.entity.Curriculum;
import com.universitymanagement.program.entity.Program;
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
public class CurriculumMapperImpl implements CurriculumMapper {

    @Override
    public CurriculumResponse toResponse(Curriculum curriculum) {
        if ( curriculum == null ) {
            return null;
        }

        UUID programId = null;
        String programName = null;
        UUID subjectId = null;
        String subjectName = null;
        String subjectCode = null;
        Double credit = null;
        UUID curriculumId = null;
        Integer semester = null;
        Integer yearLevel = null;

        programId = curriculumProgramId( curriculum );
        programName = curriculumProgramProgramName( curriculum );
        subjectId = curriculumSubjectSubjectId( curriculum );
        subjectName = curriculumSubjectSubjectName( curriculum );
        subjectCode = curriculumSubjectSubjectCode( curriculum );
        credit = curriculumSubjectCredit( curriculum );
        curriculumId = curriculum.getCurriculumId();
        semester = curriculum.getSemester();
        yearLevel = curriculum.getYearLevel();

        CurriculumResponse curriculumResponse = new CurriculumResponse( curriculumId, semester, yearLevel, programId, programName, subjectId, subjectName, subjectCode, credit );

        return curriculumResponse;
    }

    @Override
    public Curriculum toEntity(CurriculumRequest curriculumRequest) {
        if ( curriculumRequest == null ) {
            return null;
        }

        Curriculum curriculum = new Curriculum();

        curriculum.setSemester( curriculumRequest.semester() );
        curriculum.setYearLevel( curriculumRequest.yearLevel() );

        return curriculum;
    }

    private UUID curriculumProgramId(Curriculum curriculum) {
        Program program = curriculum.getProgram();
        if ( program == null ) {
            return null;
        }
        return program.getId();
    }

    private String curriculumProgramProgramName(Curriculum curriculum) {
        Program program = curriculum.getProgram();
        if ( program == null ) {
            return null;
        }
        return program.getProgramName();
    }

    private UUID curriculumSubjectSubjectId(Curriculum curriculum) {
        Subject subject = curriculum.getSubject();
        if ( subject == null ) {
            return null;
        }
        return subject.getSubjectId();
    }

    private String curriculumSubjectSubjectName(Curriculum curriculum) {
        Subject subject = curriculum.getSubject();
        if ( subject == null ) {
            return null;
        }
        return subject.getSubjectName();
    }

    private String curriculumSubjectSubjectCode(Curriculum curriculum) {
        Subject subject = curriculum.getSubject();
        if ( subject == null ) {
            return null;
        }
        return subject.getSubjectCode();
    }

    private Double curriculumSubjectCredit(Curriculum curriculum) {
        Subject subject = curriculum.getSubject();
        if ( subject == null ) {
            return null;
        }
        return subject.getCredit();
    }
}
