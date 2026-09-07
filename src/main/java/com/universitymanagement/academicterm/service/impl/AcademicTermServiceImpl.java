package com.universitymanagement.academicterm.service.impl;

import com.universitymanagement.academicterm.dto.request.AcademicTermRequest;
import com.universitymanagement.academicterm.dto.response.AcademicTermResponse;
import com.universitymanagement.academicterm.entity.AcademicTerm;
import com.universitymanagement.academicterm.exception.AcademicTermNotFoundException;
import com.universitymanagement.academicterm.repository.AcademicTermRepository;
import com.universitymanagement.academicterm.service.AcademicTermService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicTermServiceImpl implements AcademicTermService {

    private final AcademicTermRepository academicTermRepository;

    @Override
    @Transactional
    public AcademicTermResponse create(AcademicTermRequest request) {
        AcademicTerm term = new AcademicTerm();
        apply(term, request);
        return toResponse(academicTermRepository.save(term));
    }

    @Override
    @Transactional
    public AcademicTermResponse update(UUID termId, AcademicTermRequest request) {
        AcademicTerm term = findOrThrow(termId);
        apply(term, request);
        return toResponse(academicTermRepository.save(term));
    }

    @Override
    public List<AcademicTermResponse> getAll() {
        return academicTermRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public AcademicTermResponse getById(UUID termId) {
        return toResponse(findOrThrow(termId));
    }

    @Override
    @Transactional
    public void delete(UUID termId) {
        AcademicTerm term = findOrThrow(termId);
        academicTermRepository.delete(term);
    }

    private AcademicTerm findOrThrow(UUID termId) {
        return academicTermRepository.findById(termId)
                .orElseThrow(() -> new AcademicTermNotFoundException(termId));
    }

    private void apply(AcademicTerm term, AcademicTermRequest request) {
        term.setName(request.name());
        term.setAcademicYear(request.academicYear());
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term.setAddDropDeadline(request.addDropDeadline());
        term.setIsActive(request.isActive() != null ? request.isActive() : Boolean.TRUE);
    }

    private AcademicTermResponse toResponse(AcademicTerm term) {
        return new AcademicTermResponse(
                term.getTermId(),
                term.getName(),
                term.getAcademicYear(),
                term.getStartDate(),
                term.getEndDate(),
                term.getAddDropDeadline(),
                term.getIsActive()
        );
    }
}
