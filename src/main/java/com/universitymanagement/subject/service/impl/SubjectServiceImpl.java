package com.universitymanagement.subject.service.impl;

import com.universitymanagement.department.entity.Department;
import com.universitymanagement.department.repository.DepartmentRepository;
import com.universitymanagement.subject.dto.request.SubjectRequest;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.mapper.SubjectMapper;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.subject.service.SubjectService;
import jakarta.transaction.Transactional;
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
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final DepartmentRepository departmentRepository;
    @Override
    public Page<SubjectResponse> getAllSubjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return subjectRepository.findAll(pageable).map(subjectMapper::toResponse);
    }

    @Override
    public SubjectResponse getSubjectById(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        return subjectMapper.toResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest subjectRequest) {
        Department department = departmentRepository.findById(subjectRequest.departmentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        String subjectCode = generateSubjectCode(department);
        Subject savedSubject = subjectMapper.toEntity(subjectRequest);
        savedSubject.setSubjectCode(subjectCode);
        return subjectMapper.toResponse(subjectRepository.save(savedSubject));
    }

    @Override
    public SubjectResponse updateSubject(UUID subjectId, SubjectRequest subjectRequest) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        Department department = departmentRepository.findById(subjectRequest.departmentId())
                .filter(dept -> !Boolean.TRUE.equals(dept.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Department not found with id: " + subjectRequest.departmentId()));

        subject.setSubjectName(subjectRequest.subjectName());
        subject.setCredit(subjectRequest.credit());
        subject.setDepartment(department);
        Subject updatedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(updatedSubject);
    }

    @Override
    public void deleteSubject(UUID subjectId) {
    Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
    subjectRepository.delete(subject);
    }

    @Override
    public void softDelete(UUID subjectId){
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found with id: " + subjectId));
        subject.setIsDeleted(true);
        subjectRepository.save(subject);
    }

    private String generateSubjectCode(Department department) {
        String prefix = department.getDepartmentCode() != null
                ? department.getDepartmentCode().toUpperCase()
                : "SUB";
        long count = subjectRepository.countByDepartment_DepartmentId(department.getDepartmentId()) + 1;
        return String.format("%s-%03d", prefix, count);
    }
}
