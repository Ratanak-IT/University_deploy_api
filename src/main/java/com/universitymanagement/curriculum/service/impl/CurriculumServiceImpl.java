package com.universitymanagement.curriculum.service.impl;

import com.universitymanagement.curriculum.dto.request.CurriculumRequest;
import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import com.universitymanagement.curriculum.entity.Curriculum;
import com.universitymanagement.curriculum.exception.CurriculumNotFoundException;
import com.universitymanagement.curriculum.exception.DuplicateCurriculumException;
import com.universitymanagement.curriculum.mapper.CurriculumMapper;
import com.universitymanagement.curriculum.repository.CurriculumRepository;
import com.universitymanagement.curriculum.service.CurriculumService;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.program.exception.ProgramNotFoundException;
import com.universitymanagement.program.repository.ProgramRepository;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.exception.SubjectNotFoundException;
import com.universitymanagement.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurriculumServiceImpl implements CurriculumService {
    private final CurriculumRepository curriculumRepository;
    private final CurriculumMapper curriculumMapper;
    private final ProgramRepository programRepository;
    private final SubjectRepository subjectRepository;

    @Override
    public Page<CurriculumResponse> getAllCurriculums(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
      return curriculumRepository.findAll(pageable).map(curriculumMapper::toResponse);

    }

    @Override
    public Page<CurriculumResponse> getCurriculumsByProgram(UUID programId, int page, int size) {
         programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException(programId));
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("yearLevel"), Sort.Order.asc("semester")));
       return curriculumRepository.findByProgram_Id(programId, pageable)
               .map(curriculumMapper::toResponse);
    }

    @Override
    public CurriculumResponse createCurriculum(CurriculumRequest request) {
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ProgramNotFoundException(request.programId()));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));
        boolean exists = curriculumRepository.existsByProgram_IdAndSubject_SubjectIdAndSemesterAndYearLevel(
                request.programId(), request.subjectId(), request.semester(), request.yearLevel());
        if (exists) {
            throw new DuplicateCurriculumException("This subject is already assigned to this program for the given semester and year level");
        }
        Curriculum curriculum = curriculumMapper.toEntity(request);
        curriculum.setProgram(program);
        curriculum.setIsDeleted(false);
        curriculum.setSubject(subject);
        Curriculum savedCurriculum = curriculumRepository.save(curriculum);
        return curriculumMapper.toResponse(savedCurriculum);
    }

    @Override
    public CurriculumResponse updateCurriculum(UUID curriculumId, CurriculumRequest request) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new CurriculumNotFoundException(curriculumId));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));
        boolean duplicateExists = curriculumRepository
                .existsByProgram_IdAndSubject_SubjectIdAndSemesterAndYearLevelAndCurriculumIdNot(
                        curriculum.getProgram().getId(),
                        request.subjectId(),
                        request.semester(),
                        request.yearLevel(),
                        curriculumId);
        if (duplicateExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This subject is already assigned to this program for the given semester and year level");
        }
        curriculum.setSemester(request.semester());
        curriculum.setYearLevel(request.yearLevel());
        curriculum.setSubject(subject);
        Curriculum updatedCurriculum = curriculumRepository.save(curriculum);
        return curriculumMapper.toResponse(updatedCurriculum);
    }

    @Override
    public void deleteCurriculum(UUID curriculumId) {
        Curriculum curriculum= curriculumRepository.findById(curriculumId).orElseThrow(() -> new CurriculumNotFoundException(curriculumId));
        curriculumRepository.delete(curriculum);
    }

    @Override
    public void softDelete(UUID curriculumId) {
        Curriculum curriculum= curriculumRepository.findById(curriculumId).orElseThrow(() -> new CurriculumNotFoundException(curriculumId));
        curriculum.setIsDeleted(true);
        curriculumRepository.save(curriculum);
    }
}
