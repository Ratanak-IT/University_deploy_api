package com.universitymanagement.department.service.impl;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.department.exception.DepartmentNotFoundException;
import com.universitymanagement.department.exception.DuplicateDepartmentException;
import com.universitymanagement.department.mapper.Departmentmapper;
import com.universitymanagement.department.repository.DepartmentRepository;
import com.universitymanagement.department.service.DepartmentService;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.mapper.TeacherMapper;
import com.universitymanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final Departmentmapper departmentmapper;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherMapper teacherMapper;
    @Override
    public DepartmentResponse createDepartment(DepartmentRequest departmentRequest) {
        if (departmentRepository.existsByDepartmentNameIgnoreCase(departmentRequest.departmentName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }
        Department department = departmentmapper.toEntity(departmentRequest);

        department.setIsDeleted(false);
        department.setDepartmentCode(generateDepartmentCode());
        Department savedDepartment = departmentRepository.save(department);
        return departmentmapper.toResponse(savedDepartment);
    }

    @Override
    public Page<DepartmentResponse> getAllDepartments(int page, int size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "departmentName");
        Pageable pageable = PageRequest.of(page, size,sort);
       return departmentRepository.findAll(pageable).map(departmentmapper::toResponse);
    }

    @Override
    public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest departmentRequest) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        String departmentName = departmentRequest.departmentName().trim();
        if (!department.getDepartmentName().equalsIgnoreCase(departmentName)
        && departmentRepository.existsByDepartmentNameIgnoreCase(departmentName)) {
            throw new DuplicateDepartmentException("Department with name " + departmentName + " already exists");
        }
        department.setDepartmentName(departmentName);
        Department updatedDepartment = departmentRepository.save(department);
        return departmentmapper.toResponse(updatedDepartment);
    }

    @Override
    public void delete(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        departmentRepository.delete(department);
    }

    @Override
    public void softDelete(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        department.setIsDeleted(true);
        departmentRepository.save(department);
    }

    @Override
    public DepartmentResponse getDepartmentById(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        return departmentmapper.toResponse(department);
    }
    @Override
    public Page<DepartmentResponse> getByStatus(int page, int size, Boolean isDeleted) {
        Pageable pageable = PageRequest.of(page, size);
        Boolean filterValue = (isDeleted != null) ? isDeleted : Boolean.FALSE;
        return departmentRepository.findByIsDeleted(filterValue, pageable)
                .map(departmentmapper::toResponse);
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
