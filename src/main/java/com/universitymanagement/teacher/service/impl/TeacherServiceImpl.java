package com.universitymanagement.teacher.service.impl;

import com.universitymanagement.admin.service.UserManageService;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.mapper.ClassroomMapper;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.department.repository.DepartmentRepository;
import com.universitymanagement.identity.auth.dto.request.CreateUserRequest;
import com.universitymanagement.identity.auth.dto.response.CreateUserResponse;
import com.universitymanagement.identity.auth.keycloak.client.KeycloakClient;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.enums.RoleName;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.mapper.SubjectMapper;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.teacher.dto.request.AssignClassroomRequest;
import com.universitymanagement.teacher.dto.request.AssignSubjectRequest;
import com.universitymanagement.teacher.dto.request.CreateTeacherRequest;
import com.universitymanagement.teacher.dto.request.UpdateTeacherRequest;
import com.universitymanagement.teacher.dto.response.TeacherDetailResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.mapper.TeacherMapper;
import com.universitymanagement.teacher.repository.TeacherRepository;
import com.universitymanagement.teacher.service.TeacherService;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {
    private final Keycloak keycloak;
    private final KeycloakClient keycloakClient;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final UserManageService userManageService;
    private final TeacherMapper teacherMapper;
    private final SubjectMapper subjectMapper;
    private final ClassroomMapper classroomMapper;
    private final DepartmentRepository departmentRepository;

    @Value("${keycloak.target-realm}")
    private String realm;

    @Override
    public Page<TeacherResponse> getAllTeachers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return teacherRepository.findAll(pageable).map(teacherMapper::toResponse);
    }

    @Override
    public TeacherResponse getTeacherById(UUID teacherId) {
        return teacherMapper.toResponse(findTeacher(teacherId));
    }

    @Override
    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        CreateUserRequest createUserRequest = new CreateUserRequest(
                request.email(),
                request.password(),
                request.confirmPassword(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.gender(),
                RoleName.TEACHER
        );
        CreateUserResponse createdUser = userManageService.createUser(createUserRequest);

        Teacher teacher = teacherRepository.findByUserId(createdUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Teacher profile was not created for user: " + createdUser.id()));

        teacher.setSpecialization(request.specialization());
        if (request.departmentIds() != null && !request.departmentIds().isEmpty()) {
            teacher.setDepartments(resolveDepartments(request.departmentIds()));
        }

        teacher.setPosition(request.position());

        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public TeacherResponse updateTeacher(UUID teacherId, UpdateTeacherRequest request) {
        Teacher teacher = findTeacher(teacherId);

        if (request.specialization() != null) {
            teacher.setSpecialization(request.specialization());
        }
        if (request.departmentIds() != null) {
            teacher.setDepartments(resolveDepartments(request.departmentIds()));
        }
        if (request.position() != null) {
            teacher.setPosition(request.position());
        }
        if (request.hireDate() != null) {
            teacher.setHireDate(request.hireDate());
        }
        if (request.employmentStatus() != null) {
            teacher.setEmploymentStatus(request.employmentStatus());
        }

        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public void deleteTeacher(UUID teacherId) {
        Teacher teacher = findTeacher(teacherId);

        List<Classroom> classrooms = classroomRepository.findByTeacher_TeacherId(teacherId);
        classrooms.forEach(classroom -> classroom.setTeacher(null));
        classroomRepository.saveAll(classrooms);

        teacher.getSubjects().clear();

        User user = teacher.getUser();
        teacherRepository.delete(teacher);

        if (user != null) {
            keycloakClient.deleteUser(user.getKeycloakId());
            userRepository.delete(user);
        }
    }

    @Override
    public List<ClassroomResponse> getAssignedClasses(UUID teacherId) {
        findTeacher(teacherId);
        return classroomRepository.findByTeacher_TeacherIdAndIsDeletedFalse(teacherId)
                .stream()
                .map(classroomMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TeacherResponse assignSubject(UUID teacherId, AssignSubjectRequest request) {
        Teacher teacher = findTeacher(teacherId);
        Subject subject = subjectRepository.findById(request.subjectId())
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subject not found with id: " + request.subjectId()));

        if (!teacher.getSubjects().add(subject)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Subject is already assigned to this teacher");
        }

        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public TeacherResponse unassignSubject(UUID teacherId, UUID subjectId) {
        Teacher teacher = findTeacher(teacherId);
        boolean removed = teacher.getSubjects()
                .removeIf(subject -> subject.getSubjectId().equals(subjectId));

        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Subject is not assigned to this teacher");
        }

        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Override
    public List<SubjectResponse> getAssignedSubjects(UUID teacherId) {
        return findTeacher(teacherId).getSubjects()
                .stream()
                .map(subjectMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClassroomResponse assignClassroom(UUID teacherId, AssignClassroomRequest request) {
        Teacher teacher = findTeacher(teacherId);
        Classroom classroom = classroomRepository.findById(request.classroomId())
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Classroom not found with id: " + request.classroomId()));

        classroom.setTeacher(teacher);
        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }


    @Override
    public TeacherDetailResponse findTeacherByUserId(String id) {
        UserRepresentation kcUser = requireKeycloakUser(id);
        List<String> roles = fetchRealmRoles(id);

        User user = userRepository.findByKeycloakId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local DB: " + id));

        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found for user: " + id));

        return new TeacherDetailResponse(
                kcUser.getId(),
                kcUser.getUsername(),
                kcUser.getEmail(),
                kcUser.getFirstName(),
                kcUser.getLastName(),
                Boolean.TRUE.equals(kcUser.isEnabled()),
                roles,
                teacher.getTeacherCode(),
                teacher.getDepartments().stream()
                        .map(teacherMapper::toDepartmentResponse)
                        .toList(),
                teacher.getPosition(),
                teacher.getSpecialization(),
                teacher.getHireDate(),
                teacher.getEmploymentStatus()
        );
    }

    private Teacher findTeacher(UUID teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Teacher not found with id: " + teacherId));
    }

    private UserRepresentation requireKeycloakUser(String id) {
        UserRepresentation user = keycloakClient.findUserById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        return user;
    }

    private List<String> fetchRealmRoles(String id) {
        try {
            return keycloak.realm(realm)
                    .users()
                    .get(id)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .toList();
        } catch (NotFoundException e) {
            return List.of();
        }
    }

    @Override
    @Transactional
    public TeacherResponse assignDepartment(UUID teacherId, UUID departmentId) {
        Teacher teacher = findTeacher(teacherId);
        Department department = departmentRepository.findById(departmentId)
                .filter(dept -> !Boolean.TRUE.equals(dept.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Department not found with id: " + departmentId));

        if (!teacher.getDepartments().add(department)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department is already assigned to this teacher");
        }
        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public TeacherResponse unassignDepartment(UUID teacherId, UUID departmentId) {
        Teacher teacher = findTeacher(teacherId);
        boolean removed = teacher.getDepartments()
                .removeIf(dept -> dept.getDepartmentId().equals(departmentId));

        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Department is not assigned to this teacher");
        }
        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }
    private Set<Department> resolveDepartments(List<UUID> departmentIds) {
        return departmentIds.stream()
                .map(id -> departmentRepository.findById(id)
                        .filter(dept -> !Boolean.TRUE.equals(dept.getIsDeleted()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Department not found with id: " + id)))
                .collect(Collectors.toSet());
    }
}