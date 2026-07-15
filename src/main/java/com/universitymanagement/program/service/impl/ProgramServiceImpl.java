package com.universitymanagement.program.service.impl;

import com.universitymanagement.program.dto.request.ProgramRequest;
import com.universitymanagement.program.dto.response.ProgramResponse;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.program.exception.DuplicateProgramException;
import com.universitymanagement.program.exception.ProgramNotFoundException;
import com.universitymanagement.program.mapper.ProgramMapper;
import com.universitymanagement.program.repository.ProgramRepository;
import com.universitymanagement.program.service.ProgramService;
import com.universitymanagement.student.dto.response.StudentResponse;
import com.universitymanagement.student.mapper.StudentMapper;
import com.universitymanagement.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {
    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;


    @Override
    public ProgramResponse create(ProgramRequest request) {
        String programName = request.programName().trim();
        if (programRepository.existsByProgramNameIgnoreCase(programName)) {
            throw new DuplicateProgramException("Program '" + programName + "' already exists.");
        }

        Program program = programMapper.toEntity(request);
        program.setProgramName(programName);
        Program savedProgram = programRepository.save(program);
        return programMapper.toResponse(savedProgram);
    }

    @Override
    public ProgramResponse getById(UUID id) {
        Program program = programRepository.findById(id).orElseThrow(() -> new ProgramNotFoundException(id));
        return programMapper.toResponse(program);
    }

    @Override
    public Page<ProgramResponse> getAll(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return programRepository.findAll(pageable).map(programMapper::toResponse);
    }

    @Override
    public ProgramResponse update(UUID id, ProgramRequest request) {
        Program program = programRepository.findById(id).orElseThrow(() -> new ProgramNotFoundException(id));
        String programName = request.programName().trim();
        if (!program.getProgramName().equalsIgnoreCase(programName)
                && programRepository.existsByProgramNameIgnoreCase(programName)) {
            throw  new DuplicateProgramException("Program '" + programName + "' already exists.");
        }
        program.setProgramName(programName);
        program.setDegreeLevel(request.degreeLevel());
        program.setDurationYears(request.durationYears());

        Program updatedProgram = programRepository.save(program);
        return programMapper.toResponse(updatedProgram);
    }

    @Override
    public void delete(UUID id) {
        Program program = programRepository.findById(id).orElseThrow(() -> new ProgramNotFoundException(id));
        programRepository.deleteById(id);
    }

    @Override
    public List<StudentResponse> getStudentsByProgram(UUID programId) {
        if (!programRepository.existsById(programId)) {
            throw new ProgramNotFoundException(programId);
        }

        return studentRepository.findByProgram_Id(programId)
                .stream()
                .map(studentMapper::toResponse)
                .toList();
    }
}
