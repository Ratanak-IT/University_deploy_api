package com.universitymanagement.curriculum.service.impl;

import com.universitymanagement.curriculum.dto.request.CurriculumRequest;
import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import com.universitymanagement.curriculum.dto.response.CurriculumStructureResponse;
import com.universitymanagement.curriculum.entity.CourseType;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only by default — mapToRichResponse reads a curriculum's program and
 * subject, both lazy, and this class had no transaction anywhere. That was
 * invisible while open-in-view kept a session open for the whole request;
 * without it, the public curriculum list (the landing page's own endpoint)
 * would throw LazyInitializationException on the first row. The three write
 * methods override this with their own plain @Transactional.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumServiceImpl implements CurriculumService {
    private final CurriculumRepository curriculumRepository;
    private final CurriculumMapper curriculumMapper;
    private final ProgramRepository programRepository;
    private final SubjectRepository subjectRepository;

    private CurriculumResponse mapToRichResponse(Curriculum c) {
        CurriculumResponse basic = curriculumMapper.toResponse(c);
        String prereqName = null;
        String prereqCode = null;

        if (c.getPrerequisiteSubjectId() != null) {
            Optional<Subject> prereq = subjectRepository.findById(c.getPrerequisiteSubjectId());
            if (prereq.isPresent()) {
                prereqName = prereq.get().getSubjectName();
                prereqCode = prereq.get().getSubjectCode();
            }
        }

        CourseType type = c.getCourseType() != null ? c.getCourseType() : CourseType.CORE_REQUIRED;

        return new CurriculumResponse(
                basic.curriculumId(),
                basic.semester(),
                basic.yearLevel(),
                basic.programId(),
                basic.programName(),
                basic.subjectId(),
                basic.subjectName(),
                basic.subjectCode(),
                basic.credit(),
                type,
                c.getPrerequisiteSubjectId(),
                prereqName,
                prereqCode,
                c.getLectureHours(),
                c.getLabHours()
        );
    }

    @Override
    public Page<CurriculumResponse> getAllCurriculums(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return curriculumRepository.findAll(pageable).map(this::mapToRichResponse);
    }

    @Override
    public Page<CurriculumResponse> getCurriculumsByProgram(UUID programId, int page, int size) {
        programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException(programId));
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("yearLevel"), Sort.Order.asc("semester")));
        return curriculumRepository.findByProgram_Id(programId, pageable)
                .map(this::mapToRichResponse);
    }

    @Override
    public CurriculumStructureResponse getCurriculumStructureByProgram(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException(programId));

        List<Curriculum> list = curriculumRepository
                .findByProgram_IdAndIsDeletedFalseOrderByYearLevelAscSemesterAsc(programId);

        List<CurriculumResponse> richList = list.stream().map(this::mapToRichResponse).toList();

        Map<String, List<CurriculumResponse>> grouped = new LinkedHashMap<>();
        double totalCredits = 0.0;
        double coreCredits = 0.0;
        double generalCredits = 0.0;
        double electiveCredits = 0.0;
        double thesisCredits = 0.0;

        for (CurriculumResponse item : richList) {
            double c = item.credit() != null ? item.credit() : 0.0;
            totalCredits += c;

            CourseType type = item.courseType() != null ? item.courseType() : CourseType.CORE_REQUIRED;
            switch (type) {
                case CORE_REQUIRED -> coreCredits += c;
                case GENERAL_EDUCATION -> generalCredits += c;
                case ELECTIVE -> electiveCredits += c;
                case THESIS_INTERNSHIP -> thesisCredits += c;
            }

            String key = item.yearLevel() + "-" + item.semester();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<CurriculumStructureResponse.SemesterGroupResponse> semesterGroups = new ArrayList<>();
        for (Map.Entry<String, List<CurriculumResponse>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int year = Integer.parseInt(parts[0]);
            int sem = Integer.parseInt(parts[1]);
            List<CurriculumResponse> subjects = entry.getValue();
            double semCredits = subjects.stream().mapToDouble(s -> s.credit() != null ? s.credit() : 0.0).sum();

            semesterGroups.add(new CurriculumStructureResponse.SemesterGroupResponse(
                    year, sem, semCredits, subjects
            ));
        }

        return new CurriculumStructureResponse(
                program.getId(),
                program.getProgramName(),
                totalCredits,
                coreCredits,
                generalCredits,
                electiveCredits,
                thesisCredits,
                semesterGroups
        );
    }

    @Override
    @Transactional
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
        curriculum.setSubject(subject);
        curriculum.setIsDeleted(false);
        if (request.courseType() != null) curriculum.setCourseType(request.courseType());
        if (request.prerequisiteSubjectId() != null) curriculum.setPrerequisiteSubjectId(request.prerequisiteSubjectId());
        if (request.lectureHours() != null) curriculum.setLectureHours(request.lectureHours());
        if (request.labHours() != null) curriculum.setLabHours(request.labHours());

        Curriculum savedCurriculum = curriculumRepository.save(curriculum);
        return mapToRichResponse(savedCurriculum);
    }

    @Override
    @Transactional
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
        if (request.courseType() != null) curriculum.setCourseType(request.courseType());
        if (request.prerequisiteSubjectId() != null) curriculum.setPrerequisiteSubjectId(request.prerequisiteSubjectId());
        if (request.lectureHours() != null) curriculum.setLectureHours(request.lectureHours());
        if (request.labHours() != null) curriculum.setLabHours(request.labHours());

        Curriculum updatedCurriculum = curriculumRepository.save(curriculum);
        return mapToRichResponse(updatedCurriculum);
    }

    @Override
    @Transactional
    public void deleteCurriculum(UUID curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new CurriculumNotFoundException(curriculumId));
        curriculumRepository.delete(curriculum);
    }

    @Override
    public void softDelete(UUID curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new CurriculumNotFoundException(curriculumId));
        curriculum.setIsDeleted(true);
        curriculumRepository.save(curriculum);
    }
}
