package com.universitymanagement.program.service.impl;

import com.universitymanagement.curriculum.repository.CurriculumRepository;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.department.repository.DepartmentRepository;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;
    private final CurriculumRepository curriculumRepository;
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final DepartmentRepository departmentRepository;

    @Override
    public ProgramResponse create(ProgramRequest request) {
        String programName = request.programName().trim();
        if (programRepository.existsByProgramNameIgnoreCase(programName)) {
            throw new DuplicateProgramException("Program '" + programName + "' already exists.");
        }

        Program program = programMapper.toEntity(request);
        program.setProgramName(programName);

        if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId()).orElse(null);
            program.setDepartment(dept);
        }

        Program savedProgram = programRepository.save(program);
        return programMapper.toResponse(savedProgram);
    }

    @Override
    public ProgramResponse getById(UUID id) {
        Program program = programRepository.findById(id).orElseThrow(() -> new ProgramNotFoundException(id));

        return programMapper.toResponse(program).withCounts(
                countOf(curriculumRepository.countSubjectsByProgram().stream()
                        .collect(Collectors.toMap(
                                CurriculumRepository.ProgramSubjectCount::getProgramId,
                                CurriculumRepository.ProgramSubjectCount::getTotal)), id),
                countOf(studentRepository.countStudentsByProgram().stream()
                        .collect(Collectors.toMap(
                                StudentRepository.ProgramStudentCount::getProgramId,
                                StudentRepository.ProgramStudentCount::getTotal)), id));
    }

    @Override
    public Page<ProgramResponse> getAll(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);

        // Two grouped counts for the whole table, rather than two per row.
        Map<UUID, Long> subjectCounts = curriculumRepository.countSubjectsByProgram().stream()
                .collect(Collectors.toMap(
                        CurriculumRepository.ProgramSubjectCount::getProgramId,
                        CurriculumRepository.ProgramSubjectCount::getTotal));

        Map<UUID, Long> studentCounts = studentRepository.countStudentsByProgram().stream()
                .collect(Collectors.toMap(
                        StudentRepository.ProgramStudentCount::getProgramId,
                        StudentRepository.ProgramStudentCount::getTotal));

        return programRepository.findAll(pageable)
                .map(programMapper::toResponse)
                .map(response -> response.withCounts(
                        countOf(subjectCounts, response.id()),
                        countOf(studentCounts, response.id())));
    }

    private int countOf(Map<UUID, Long> counts, UUID id) {
        return counts.getOrDefault(id, 0L).intValue();
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

        if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId()).orElse(null);
            program.setDepartment(dept);
        }

        Program updatedProgram = programRepository.save(program);
        return programMapper.toResponse(updatedProgram);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Program program = programRepository.findById(id).orElseThrow(() -> new ProgramNotFoundException(id));

        try { entityManager.createNativeQuery("UPDATE students SET program_id = NULL WHERE program_id = :id").setParameter("id", id).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("UPDATE classrooms SET program_id = NULL WHERE program_id = :id").setParameter("id", id).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("DELETE FROM curriculum_structures WHERE program_id = :id").setParameter("id", id).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("DELETE FROM curriculums WHERE program_id = :id").setParameter("id", id).executeUpdate(); } catch (Exception ignored) {}

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
