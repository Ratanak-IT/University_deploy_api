package com.universitymanagement.department.service.impl;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.department.exception.DepartmentNotFoundException;
import com.universitymanagement.department.exception.DuplicateDepartmentException;
import com.universitymanagement.department.mapper.Departmentmapper;
import com.universitymanagement.department.repository.DepartmentCount;
import com.universitymanagement.department.repository.DepartmentRepository;
import com.universitymanagement.department.service.DepartmentService;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.mapper.TeacherMapper;
import com.universitymanagement.teacher.repository.TeacherRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only by default; the write methods below each carry their own plain
 * {@code @Transactional}. Without a session, {@code getAllDepartments} threw
 * LazyInitializationException the moment open-in-view stopped papering over
 * it — {@code Department.subjects} is lazy, and the mapper reads its size
 * when building each {@code DepartmentResponse}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    @PersistenceContext
    private EntityManager entityManager;

    private final DepartmentRepository departmentRepository;
    private final Departmentmapper departmentmapper;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherMapper teacherMapper;
    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest departmentRequest) {
        if (departmentRepository.existsByDepartmentNameIgnoreCase(departmentRequest.departmentName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }
        Department department = departmentmapper.toEntity(departmentRequest);

        department.setIsDeleted(false);
        department.setDepartmentCode(generateDepartmentCode());
        Department savedDepartment = departmentRepository.save(department);
        // Nothing has been assigned to a department that was created a moment
        // ago, so counting would be a query to learn zero.
        return departmentmapper.toResponse(savedDepartment, 0, 0);
    }

    @Override
    public Page<DepartmentResponse> getAllDepartments(int page, int size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "departmentName");
        Pageable pageable = PageRequest.of(page, size,sort);
        return withCounts(departmentRepository.findAll(pageable));
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest departmentRequest) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        String departmentName = departmentRequest.departmentName().trim();
        if (!department.getDepartmentName().equalsIgnoreCase(departmentName)
        && departmentRepository.existsByDepartmentNameIgnoreCase(departmentName)) {
            throw new DuplicateDepartmentException("Department with name " + departmentName + " already exists");
        }
        department.setDepartmentName(departmentName);
        Department updatedDepartment = departmentRepository.save(department);
        return withCounts(updatedDepartment);
    }

    @Override
    @Transactional
    public void delete(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));

        try { entityManager.createNativeQuery("DELETE FROM teacher_departments WHERE department_id = :id").setParameter("id", departmentId).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("UPDATE programs SET department_id = NULL WHERE department_id = :id").setParameter("id", departmentId).executeUpdate(); } catch (Exception ignored) {}
        try { entityManager.createNativeQuery("UPDATE subjects SET department_id = NULL WHERE department_id = :id").setParameter("id", departmentId).executeUpdate(); } catch (Exception ignored) {}

        departmentRepository.delete(department);
    }

    @Override
    @Transactional
    public void softDelete(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        department.setIsDeleted(true);
        departmentRepository.save(department);
    }

    @Override
    public DepartmentResponse getDepartmentById(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        return withCounts(department);
    }
    @Override
    public Page<DepartmentResponse> getByStatus(int page, int size, Boolean isDeleted) {
        Pageable pageable = PageRequest.of(page, size);
        Boolean filterValue = (isDeleted != null) ? isDeleted : Boolean.FALSE;
        return withCounts(departmentRepository.findByIsDeleted(filterValue, pageable));
    }

    @Override
    public List<TeacherResponse> getTeachersByDepartment(UUID departmentId) {
        departmentRepository.findById(departmentId)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId));

        return teacherRepository.findByDepartments_DepartmentId(departmentId)
                .stream()
                .map(teacherMapper::toResponse)
                .toList();
    }

    /**
     * Counts a whole page in two queries rather than two per row.
     *
     * <p>A page of 25 departments would otherwise cost 50 round trips to answer
     * a question the database can answer for all of them at once.
     */
    private Page<DepartmentResponse> withCounts(Page<Department> departments) {
        if (departments.isEmpty()) {
            return departments.map(d -> departmentmapper.toResponse(d, 0, 0));
        }

        Map<UUID, Long> teachers = toMap(departmentRepository.countTeachersPerDepartment());
        Map<UUID, Long> subjects = toMap(departmentRepository.countSubjectsPerDepartment());

        return departments.map(d -> departmentmapper.toResponse(
                d,
                teachers.getOrDefault(d.getDepartmentId(), 0L),
                subjects.getOrDefault(d.getDepartmentId(), 0L)));
    }

    private DepartmentResponse withCounts(Department department) {
        UUID id = department.getDepartmentId();
        return departmentmapper.toResponse(
                department,
                departmentRepository.countTeachersIn(id),
                departmentRepository.countSubjectsIn(id));
    }

    private Map<UUID, Long> toMap(List<DepartmentCount> counts) {
        return counts.stream().collect(Collectors.toMap(
                DepartmentCount::departmentId, DepartmentCount::total));
    }

    private String generateDepartmentCode() {
        return departmentRepository.findFirstByOrderByDepartmentIdDesc()
                .map(lastDept -> {
                    String lastCode = lastDept.getDepartmentCode();
                    if (lastCode == null || !lastCode.startsWith("DEP-")) {
                        return "DEP-01";
                    }
                    try {
                        int lastNumber = Integer.parseInt(lastCode.substring(4));
                        return String.format("DEP-%02d", lastNumber + 1);
                    } catch (NumberFormatException e) {
                        return "DEP-01";
                    }
                })
                .orElse("DEP-01");
    }
}
