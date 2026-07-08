package com.universitymanagement.program.service.impl;

import com.universitymanagement.program.dto.request.ProgramRequest;
import com.universitymanagement.program.dto.response.ProgramResponse;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.program.mapper.ProgramMapper;
import com.universitymanagement.program.repository.ProgramRepository;
import com.universitymanagement.program.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {
    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;


    @Override
    public ProgramResponse create(ProgramRequest request) {
        String programName = request.programName().trim();
        if (programRepository.existsByProgramNameIgnoreCase(programName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Program already exists");
        }

        Program program = programMapper.toEntity(request);
        program.setProgramName(programName);
        Program savedProgram = programRepository.save(program);
        return programMapper.toResponse(savedProgram);
    }

    @Override
    public ProgramResponse getById(UUID id) {
        Program program = programRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
        return programMapper.toResponse(program);
    }

    @Override
    public Page<ProgramResponse> getAll(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return programRepository.findAll(pageable).map(programMapper::toResponse);
    }

    @Override
    public ProgramResponse update(UUID id, ProgramRequest request) {
        Program program = programRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found with id:"+ id));
        String programName = request.programName().trim();
        if (!program.getProgramName().equalsIgnoreCase(programName)
                && programRepository.existsByProgramNameIgnoreCase(programName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Program '" + programName + "' already exists.");
        }
        program.setProgramName(programName);
        program.setDegreeLevel(request.degreeLevel());
        program.setDurationYears(request.durationYears());

        Program updatedProgram = programRepository.save(program);
        return programMapper.toResponse(updatedProgram);
    }

    @Override
    public void delete(UUID id) {
        programRepository.deleteById(id);
    }
}
