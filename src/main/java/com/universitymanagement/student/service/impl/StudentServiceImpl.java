package com.universitymanagement.student.service.impl;

import com.universitymanagement.admin.service.UserManageService;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.identity.auth.dto.request.CreateUserRequest;
import com.universitymanagement.identity.auth.dto.response.CreateUserResponse;
import com.universitymanagement.identity.auth.keycloak.client.KeycloakClient;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.enums.RoleName;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.identity.service.UserProfileWriter;
import com.universitymanagement.minio.MinioService;
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
import com.universitymanagement.student.service.StudentAcademicService;
import com.universitymanagement.student.service.StudentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
    private final MinioService minioService;
    private final UserProfileWriter userProfileWriter;
    @Lazy private final StudentAcademicService academicService;

    @Value("${keycloak.target-realm}")
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

        // Fill in the auto-created Student profile with the admin's values.
        Student student = studentRepository.findByUserId(createdUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Student profile was not created for user: " + createdUser.id()));

        // Person-level extras that createUser() doesn't know about.
        User user = student.getUser();
        if (user != null) {
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.syncFullName();
            user.setNameKhmer(request.nameKhmer());
            user.setIdCardNumber(request.idCardNumber());
            user.setPlaceOfBirth(request.placeOfBirth());
            user.setCurrentAddress(request.currentAddress());
            user.setAddress(request.address() != null ? request.address() : request.currentAddress());
            user.setFatherContact(request.fatherContact());
            user.setMotherContact(request.motherContact());
            userRepository.save(user);
        }

        if (request.studentCode() != null && !request.studentCode().isBlank()) {
            String code = request.studentCode().trim();
            if (!code.equals(student.getStudentCode()) && studentRepository.existsByStudentCode(code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Student code already in use: " + code);
            }
            student.setStudentCode(code);
        }

        student.setAcademicYear(request.academicYear());
        student.setYearLevel(request.yearLevel());
        student.setSemester(request.semester());
        student.setDob(request.dateOfBirth());
        student.setGender(request.gender() != null ? request.gender().name() : null);
        student.setAddress(request.currentAddress() != null ? request.currentAddress() : request.address());
        student.setFatherContact(request.fatherContact());
        student.setMotherContact(request.motherContact());
        student.setStatus("active");

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

        // 1. Person-level fields (users table + Keycloak).
        User user = student.getUser();
        if (user != null) {
            userProfileWriter.apply(user, request);
        }

        // 2. Academic fields (students table).
        if (request.studentCode() != null && !request.studentCode().isBlank()) {
            String code = request.studentCode().trim();
            if (!code.equals(student.getStudentCode()) && studentRepository.existsByStudentCode(code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Student code already in use: " + code);
            }
            student.setStudentCode(code);
        }
        if (request.academicYear() != null) {
            student.setAcademicYear(request.academicYear());
        }
        if (request.yearLevel() != null) {
            student.setYearLevel(request.yearLevel());
        }
        if (request.semester() != null) {
            student.setSemester(request.semester());
        }
        if (request.programId() != null) {
            student.setProgram(resolveProgram(request.programId()));
        }
        if (request.enrollmentDate() != null) {
            student.setEnrollmentDate(request.enrollmentDate());
        }
        if (request.graduationDate() != null) {
            student.setGraduationDate(request.graduationDate());
        }
        if (request.graduationStatus() != null) {
            student.setGraduationStatus(request.graduationStatus());
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim().toLowerCase();
            student.setStatus(status);
            if ("graduated".equals(status)) {
                student.setGraduationStatus("graduated");
            }
        }

        // 3. Keep the denormalised copies on `students` in sync so older
        //    screens reading them don't show stale data.
        if (request.dateOfBirth() != null) {
            student.setDob(request.dateOfBirth());
        }
        if (request.gender() != null) {
            student.setGender(request.gender().name());
        }
        if (request.currentAddress() != null) {
            student.setAddress(request.currentAddress());
        } else if (request.address() != null) {
            student.setAddress(request.address());
        }
        if (request.fatherContact() != null) {
            student.setFatherContact(request.fatherContact());
        }
        if (request.motherContact() != null) {
            student.setMotherContact(request.motherContact());
        }

        return studentMapper.toAdminResponse(studentRepository.save(student));
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void deleteStudent(UUID studentId) {
        Student student = findStudent(studentId);

        entityManager.createQuery("DELETE FROM Attendance a WHERE a.student.studentId = :id").setParameter("id", studentId).executeUpdate();
        entityManager.createQuery("DELETE FROM Submission s WHERE s.student.studentId = :id").setParameter("id", studentId).executeUpdate();
        entityManager.createQuery("DELETE FROM ExamScore e WHERE e.student.studentId = :id").setParameter("id", studentId).executeUpdate();
        entityManager.createQuery("DELETE FROM QuizAttempt q WHERE q.student.studentId = :id").setParameter("id", studentId).executeUpdate();
        entityManager.createQuery("DELETE FROM ClassroomStudent cs WHERE cs.student.studentId = :id").setParameter("id", studentId).executeUpdate();

        User user = student.getUser();
        studentRepository.delete(student);

        if (user != null) {
            try {
                keycloakClient.deleteUser(user.getKeycloakId());
            } catch (Exception ignored) {}
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

        if (request.address() != null) {
            user.setAddress(request.address());
            user.setCurrentAddress(request.address());
        }
        if (request.phone() != null) {
            user.setPhoneNumber(request.phone());
        }
        if (request.fatherContact() != null) {
            user.setFatherContact(request.fatherContact());
        }
        if (request.motherContact() != null) {
            user.setMotherContact(request.motherContact());
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
                student.getStudentId().toString(),
                kcUser.getUsername(),
                kcUser.getEmail(),
                user.resolvedFirstName() != null ? user.resolvedFirstName() : kcUser.getFirstName(),
                user.resolvedLastName() != null ? user.resolvedLastName() : kcUser.getLastName(),
                Boolean.TRUE.equals(kcUser.isEnabled()),
                roles,
                student.getStudentCode(),
                student.getAcademicYear(),
                student.getYearLevel(),
                student.getSemester(),
                user.getDateOfBirth(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getNameKhmer(),
                user.getIdCardNumber(),
                user.getPlaceOfBirth(),
                user.getCurrentAddress() != null ? user.getCurrentAddress() : user.getAddress(),
                user.getPhoneNumber(),
                user.getAvatarObjectName() != null ? minioService.getAssetPreviewUrl(user.getAvatarObjectName()) : null,
                student.getGraduationStatus(),
                student.getProgram() != null ? student.getProgram().getProgramName() : null
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

    @Override
    public StudentDetailResponse uploadMyAvatar(MultipartFile file) {
        User user = accessGuard.getCurrentUser();
        String objectName = minioService.uploadAsset(file);
        user.setAvatarObjectName(objectName);
        userRepository.save(user);
        return findStudentById(user.getKeycloakId());
    }
}