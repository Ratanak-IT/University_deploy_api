package com.universitymanagement.subject.service.impl;

import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.department.exception.DepartmentNotFoundException;
import com.universitymanagement.department.repository.DepartmentRepository;
import com.universitymanagement.subject.dto.request.SubjectRequest;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.exception.SubjectNotFoundException;
import com.universitymanagement.subject.mapper.SubjectMapper;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.subject.service.SubjectService;
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

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    @PersistenceContext
    private EntityManager entityManager;

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final DepartmentRepository departmentRepository;
    private final ClassroomRepository classroomRepository;
    @Override
    public Page<SubjectResponse> getAllSubjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // One grouped count for the whole table, rather than a count per row.
        Map<UUID, Long> classroomCounts = classroomRepository.countClassroomsBySubject().stream()
                .collect(Collectors.toMap(
                        ClassroomRepository.SubjectClassroomCount::getSubjectId,
                        ClassroomRepository.SubjectClassroomCount::getTotal));

        return subjectRepository.findAll(pageable)
                .map(subjectMapper::toResponse)
                .map(response -> response.withClassroomCount(
                        classroomCounts.getOrDefault(response.subjectId(), 0L).intValue()));
    }

    @Override
    public SubjectResponse getSubjectById(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new SubjectNotFoundException(subjectId));
        return subjectMapper.toResponse(subject)
                .withClassroomCount((int) classroomRepository.countBySubject_SubjectId(subjectId));
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest subjectRequest) {
        Department department = departmentRepository.findById(subjectRequest.departmentId())
                .filter(dept -> !Boolean.TRUE.equals(dept.getIsDeleted()))
                .orElseThrow(() -> new DepartmentNotFoundException(subjectRequest.departmentId()));

        String subjectCode = generateSubjectCode();
        Subject savedSubject = subjectMapper.toEntity(subjectRequest);
        savedSubject.setSubjectCode(subjectCode);
        savedSubject.setDepartment(department);
        savedSubject.setIsDeleted(false);
        return subjectMapper.toResponse(subjectRepository.save(savedSubject));
    }

    @Override
    public SubjectResponse updateSubject(UUID subjectId, SubjectRequest subjectRequest) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new SubjectNotFoundException(subjectId));
        Department department = departmentRepository.findById(subjectRequest.departmentId())
                .filter(dept -> !Boolean.TRUE.equals(dept.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Department not found with id: " + subjectRequest.departmentId()));

        subject.setSubjectName(subjectRequest.subjectName());
        subject.setCredit(subjectRequest.credit());
        if (subjectRequest.hours() != null) {
            subject.setHours(subjectRequest.hours());
        }
        subject.setDepartment(department);
        Subject updatedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(updatedSubject);
    }

    @Override
    @Transactional
    public void deleteSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new SubjectNotFoundException(subjectId));

        try { entityManager.createNativeQuery("DELETE FROM curriculum_structures WHERE subject_id = :id").setParameter("id", subjectId).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("DELETE FROM curriculum_items WHERE subject_id = :id").setParameter("id", subjectId).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("DELETE FROM teacher_subjects WHERE subject_id = :id").setParameter("id", subjectId).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("UPDATE classrooms SET subject_id = NULL WHERE subject_id = :id").setParameter("id", subjectId).executeUpdate(); } catch (Exception ignored) {}

        subjectRepository.delete(subject);
    }

    @Override
    public void softDelete(UUID subjectId){
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new SubjectNotFoundException(subjectId));
        subject.setIsDeleted(true);
        subjectRepository.save(subject);
    }

    private String generateSubjectCode() {
        return subjectRepository.findLastSubjectCode()
                .map(lastCode -> {
                    try {
                        int lastNumber = Integer.parseInt(lastCode.substring(4));
                        return String.format("SUB-%03d", lastNumber + 1);
                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                        return "SUB-001";
                    }
                })
                .orElse("SUB-001");
    }

    @Override
    public long countClassroomsBySubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));

        return classroomRepository.countBySubject_SubjectId(subject.getSubjectId());
    }
}
