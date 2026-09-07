package com.universitymanagement.student.service.impl;

import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
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
import com.universitymanagement.student.dto.response.StudentDirectoryResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.mapper.StudentMapper;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.student.security.StudentAccessGuard;
import com.universitymanagement.student.service.StudentAcademicService;
import com.universitymanagement.student.service.StudentService;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.exception.TeacherNotFoundException;
import com.universitymanagement.teacher.repository.TeacherRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Read-only by default; the write methods below each carry their own plain
 * {@code @Transactional}, which overrides this at the method level. Without
 * a session, {@code getStudentById}/the admin listing threw
 * LazyInitializationException reading a student's program the moment
 * open-in-view stopped papering over it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    private final TeacherRepository teacherRepository;
    @Lazy private final StudentAcademicService academicService;

    @Value("${keycloak.target-realm}")
    private String realm;

    @Override
    public Page<StudentAdminResponse> getAllStudents(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("enrollmentDate").descending());
        if (keyword == null || keyword.isBlank()) {
            return studentRepository.findAllLive(pageable).map(studentMapper::toAdminResponse);
        }
        return studentRepository.search(keyword.trim(), pageable)
                .map(studentMapper::toAdminResponse);
    }

    @Override
    public Page<StudentDirectoryResponse> searchStudentDirectory(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("enrollmentDate").descending());
        if (keyword == null || keyword.isBlank()) {
            return studentRepository.findAllLive(pageable).map(studentMapper::toDirectoryResponse);
        }
        return studentRepository.search(keyword.trim(), pageable)
                .map(studentMapper::toDirectoryResponse);
    }

    @Override
    public Page<StudentAdminResponse> getWithdrawnStudents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("enrollmentDate").descending());
        return studentRepository.findAllWithdrawn(pageable).map(studentMapper::toAdminResponse);
    }

    @Override
    @Transactional
    public void restoreStudent(UUID studentId) {
        Student student = findStudent(studentId);

        student.setIsDeleted(false);
        student.setStatus("active");
        studentRepository.save(student);

        enableSignIn(student.getUser());
    }

    /** Undoes {@link #disableSignIn}. */
    private void enableSignIn(User user) {
        if (user == null) {
            return;
        }

        user.setIsActive(true);
        user.setAccountStatus("ACTIVE");
        userRepository.save(user);

        if (user.getKeycloakId() == null) {
            return;
        }

        try {
            UserRepresentation kcUser = keycloakClient.findUserById(user.getKeycloakId());
            if (kcUser != null) {
                kcUser.setEnabled(true);
                keycloakClient.updateUser(kcUser);
            }
        } catch (Exception e) {
            // Loud, because the opposite failure mode of disableSignIn applies:
            // the record would read as restored while the person still cannot
            // sign in, and nobody would know why.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The record was restored but the sign-in account could not "
                            + "be re-enabled. Enable it in Keycloak.", e);
        }
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

            // The guard already refuses a suspended student on the next
            // request. Mirroring it into Keycloak takes away their tokens as
            // well, so the sanction survives the browser tab they already have
            // open — and lifting it puts everything back.
            if (BLOCKED_STATUSES.contains(status)) {
                disableSignIn(student.getUser());
            } else {
                enableSignIn(student.getUser());
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

    /**
     * Statuses that stop a student using the platform.
     *
     * <p>"graduated" is not one of them: a graduate needs their transcript and
     * certificates more than anyone, and that is exactly when they come looking.
     */
    private static final java.util.Set<String> BLOCKED_STATUSES =
            java.util.Set.of("suspended", "inactive");

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Withdraws a student without destroying what they did here.
     *
     * <p>This used to delete the row, and to make that possible it first
     * deleted every attendance record, submission, exam score, quiz attempt and
     * enrolment belonging to them. That is the university's academic record —
     * the thing a transcript is made of — and once gone a certificate could
     * never be reissued, nor a grade appeal answered. It also meant "cannot be
     * deleted as it is currently in use" whenever a foreign key was missed.
     *
     * <p>So the record is marked withdrawn and the sign-in account is disabled
     * in Keycloak. Disabling there is the part that actually locks them out:
     * no new tokens can be minted, so their session dies when the current
     * access token expires, and the refresh token stops working at once.
     */
    @Override
    @Transactional
    public void deleteStudent(UUID studentId) {
        Student student = findStudent(studentId);

        student.setIsDeleted(true);
        student.setStatus("inactive");
        studentRepository.save(student);

        disableSignIn(student.getUser());
    }

    /** Locks the person out, locally and at Keycloak. */
    private void disableSignIn(User user) {
        if (user == null) {
            return;
        }

        user.setIsActive(false);
        user.setAccountStatus("DISABLED");
        userRepository.save(user);

        if (user.getKeycloakId() == null) {
            return;
        }

        try {
            UserRepresentation kcUser = keycloakClient.findUserById(user.getKeycloakId());
            if (kcUser != null) {
                kcUser.setEnabled(false);
                keycloakClient.updateUser(kcUser);
            }

            // Disabling alone leaves whatever is already out there working —
            // the browser's session cookie and any access token still in play.
            // Ending the sessions turns "cannot sign in again" into "signed out
            // now", which is what a suspension is supposed to mean.
            keycloakClient.logoutAllSessions(user.getKeycloakId());
        } catch (Exception e) {
            // The local record is already withdrawn, so failing here would
            // leave the two halves disagreeing. Loud, because an account that
            // can still sign in is exactly what this method exists to prevent.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The record was withdrawn but the sign-in account could not "
                            + "be disabled. Disable it in Keycloak before relying on this.", e);
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
    @Transactional
    public StudentDetailResponse findStudentById(String id) {
        UserRepresentation kcUser = requireKeycloakUser(id);
        List<String> roles = fetchRealmRoles(id);

        User user = userRepository.findByKeycloakId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found in local DB: " + id));

        Student student = studentRepository.findByUserIdWithProgram(user.getId())
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
                student.getProgram() != null ? student.getProgram().getProgramName() : null,
                resolveAdvisorName(student)
        );
    }

    private String resolveAdvisorName(Student student) {
        if (student.getAdvisor() == null || student.getAdvisor().getUser() == null) {
            return null;
        }
        User advisorUser = student.getAdvisor().getUser();
        String first = advisorUser.resolvedFirstName();
        String last = advisorUser.resolvedLastName();
        if (first == null && last == null) {
            return null;
        }
        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
    }

    @Override
    @Transactional
    public StudentAdminResponse assignAdvisor(UUID studentId, UUID teacherId) {
        Student student = findStudent(studentId);
        if (teacherId == null) {
            student.setAdvisor(null);
        } else {
            Teacher teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new TeacherNotFoundException(teacherId));
            student.setAdvisor(teacher);
        }
        return studentMapper.toAdminResponse(studentRepository.save(student));
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
    @Transactional
    public StudentDetailResponse uploadMyAvatar(MultipartFile file) {
        User user = accessGuard.getCurrentUser();
        String objectName = minioService.uploadAsset(file);
        user.setAvatarObjectName(objectName);
        userRepository.save(user);
        return findStudentById(user.getKeycloakId());
    }

    @Override
    @Transactional
    public void resetPassword(UUID studentId, AdminResetPasswordRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found: " + studentId));

        User user = student.getUser();
        if (user == null || user.getKeycloakId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This student has no sign-in account to reset.");
        }

        // Delegated: the Keycloak call, the confirmation check and the
        // temporary-password flag all already live in one place.
        userManageService.resetPassword(user.getKeycloakId(), request);
    }
}
