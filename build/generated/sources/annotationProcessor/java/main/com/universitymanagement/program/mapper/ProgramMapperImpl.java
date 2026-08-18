package com.universitymanagement.program.mapper;

import com.universitymanagement.department.entity.Department;
import com.universitymanagement.program.dto.request.ProgramRequest;
import com.universitymanagement.program.dto.response.ProgramResponse;
import com.universitymanagement.program.entity.Program;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-18T15:02:11+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class ProgramMapperImpl implements ProgramMapper {

    @Override
    public ProgramResponse toResponse(Program program) {
        if ( program == null ) {
            return null;
        }

        UUID departmentId = null;
        String departmentName = null;
        UUID id = null;
        String programName = null;
        String degreeLevel = null;
        Integer durationYears = null;

        departmentId = programDepartmentDepartmentId( program );
        departmentName = programDepartmentDepartmentName( program );
        id = program.getId();
        programName = program.getProgramName();
        degreeLevel = program.getDegreeLevel();
        durationYears = program.getDurationYears();

        ProgramResponse programResponse = new ProgramResponse( id, programName, degreeLevel, durationYears, departmentId, departmentName );

        return programResponse;
    }

    @Override
    public Program toEntity(ProgramRequest programRequest) {
        if ( programRequest == null ) {
            return null;
        }

        Program program = new Program();

        program.setProgramName( programRequest.programName() );
        program.setDegreeLevel( programRequest.degreeLevel() );
        program.setDurationYears( programRequest.durationYears() );

        return program;
    }

    private UUID programDepartmentDepartmentId(Program program) {
        Department department = program.getDepartment();
        if ( department == null ) {
            return null;
        }
        return department.getDepartmentId();
    }

    private String programDepartmentDepartmentName(Program program) {
        Department department = program.getDepartment();
        if ( department == null ) {
            return null;
        }
        return department.getDepartmentName();
    }
}
