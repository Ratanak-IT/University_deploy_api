package com.universitymanagement.student.service.impl;

import com.universitymanagement.admin.service.UserManageService;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.identity.auth.dto.request.CreateUserRequest;
import com.universitymanagement.identity.auth.dto.response.CreateUserResponse;
import com.universitymanagement.identity.auth.keycloak.client.KeycloakClient;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.enums.RoleName;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.program.repository.ProgramRepository;
import com.universitymanagement.student.dto.request.CreateStudentRequest;
import com.universitymanagement.student.dto.request.StudentUpdateProfileRequest;
import com.universitymanagement.student.dto.request.StudentUpdateRequest;
import com.universitymanagement.student.dto.response.StudentAdminResponse;
import com.universitymanagement.student.dto.response.StudentDetailResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.mapper.StudentMapper;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.student.security.StudentAccessGuard;
import com.universitymanagement.student.service.StudentService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final Keycloak keycloak;
    private final KeycloakClient keycloakClient;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final UserManageService userManageService;
    private final StudentMapper studentMapper;
    private final StudentAccessGuard accessGuard;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public Page<StudentAdminResponse> getAllStudents(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("enrollmentDate").descending());
        if (keyword == null || keyword.isBlank()) {
            return studentRepository.findAll(pageable).map(studentMapper::toAdminResponse);
        }
        return studentRepository.search(keyword.trim(), pageable)
                .map(studentMapper::toAdminResponse);
    }

    @Override
    public StudentAdminResponse getStudentById(UUID studentId) {
        return studentMapper.toAdminResponse(findStudent(studentId));
    }

    @Override
    @Transactional
    public StudentAdminResponse createStudent(CreateStudentRequest request) {

        CreateUserRequest createUserRequest = new CreateUserRequest(
                request.email(),
                request.password(),
                request.confirmPassword(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.gender(),
                RoleName.STUDENT
        );
        CreateUserResponse createdUser = userManageService.createUser(createUserRequest);

        //   the auto-created Student profile with the admin's values.
        Student student = studentRepository.findByUserId(createdUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Student profile was not created for user: " + createdUser.id()));

        student.setAcademicYear(request.academicYear());
        student.setYearLevel(request.yearLevel());
        student.setSemester(request.semester());
        if (request.enrollmentDate() != null) {
            student.setEnrollmentDate(request.enrollmentDate());
        }
        if (request.programId() != null) {
            student.setProgram(resolveProgram(request.programId()));
        }

        return studentMapper.toAdminResponse(studentRepository.save(student));
    }

    @Override
    @Transactional
    public StudentAdminResponse updateStudent(UUID studentId, StudentUpdateRequest request) {
        Student student = findStudent(studentId);
        User user = student.getUser();

        if (request.getAcademicYear() != null) {
            student.setAcademicYear(request.getAcademicYear());
        }
        if (request.getYearLevel() != null) {
            student.setYearLevel(request.getYearLevel());
        }
        if (request.getSemester() != null) {
            student.setSemester(request.getSemester());
        }
        if (request.getProgramId() != null) {
            student.setProgram(resolveProgram(request.getProgramId()));
        }

        if (user != null) {
            if (request.getDob() != null) {
                user.setDateOfBirth(request.getDob());
            }
            if (request.getAddress() != null) {
                user.setAddress(request.getAddress());
            }
            userRepository.save(user);
        }

        return studentMapper.toAdminResponse(studentRepository.save(student));
    }

    @Override
    @Transactional
    public void deleteStudent(UUID studentId) {
        Student student = findStudent(studentId);

        // Detach classroom enrollments first (FK constraint)
        classroomStudentRepository.deleteAll(
                classroomStudentRepository.findByStudent_StudentId(studentId));

        User user = student.getUser();
        studentRepository.delete(student);

        if (user != null) {
            keycloakClient.deleteUser(user.getKeycloakId());
            userRepository.delete(user);
        }
    }

    @Override
    public StudentDetailResponse getMyProfile() {
        User user = accessGuard.getCurrentUser();
        return findStudentById(user.getKeycloakId());
    }

    @Override
    @Transactional
    public StudentDetailResponse updateMyProfile(StudentUpdateProfileRequest request) {
        User user = accessGuard.getCurrentUser();

        if (request.getDob() != null) {
            user.setDateOfBirth(request.getDob());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            user.setPhoneNumber(request.getPhone());
        }
        userRepository.save(user);

        return findStudentById(user.getKeycloakId());
    }

    @Override
    public StudentDetailResponse findStudentById(String id) {
        UserRepresentation kcUser = requireKeycloakUser(id);
        List<String> roles = fetchRealmRoles(id);

        User user = userRepository.findByKeycloakId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found in local DB: " + id));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student profile not found for user: " + id));

        return new StudentDetailResponse(
                kcUser.getId(),
                kcUser.getUsername(),
                kcUser.getEmail(),
                kcUser.getFirstName(),
                kcUser.getLastName(),
                Boolean.TRUE.equals(kcUser.isEnabled()),
                roles,
                student.getStudentCode(),
                student.getAcademicYear(),
                student.getYearLevel(),
                student.getSemester(),
                user.getDateOfBirth(),
                user.getGender() != null ? user.getGender().name() : null,
                student.getGraduationStatus()
        );
    }

    private Student findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with id: " + studentId));
    }

    private Program resolveProgram(UUID programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Program not found with id: " + programId));
    }

    private UserRepresentation requireKeycloakUser(String id) {
        UserRepresentation user = keycloakClient.findUserById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found with id: " + id);
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
}
