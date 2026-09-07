package com.universitymanagement.teacher.service.impl;

import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
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
import com.universitymanagement.identity.service.UserProfileWriter;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.mapper.SubjectMapper;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.teacher.dto.request.AssignClassroomRequest;
import com.universitymanagement.teacher.dto.request.AssignSubjectRequest;
import com.universitymanagement.teacher.dto.request.CreateTeacherRequest;
import com.universitymanagement.teacher.dto.request.UpdateTeacherRequest;
import com.universitymanagement.teacher.dto.response.TeacherDashboardSummaryResponse;
import com.universitymanagement.teacher.dto.response.TeacherDetailResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.mapper.TeacherMapper;
import com.universitymanagement.teacher.repository.TeacherRepository;
import com.universitymanagement.teacher.service.TeacherService;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.lesson.repository.LessonRepository;
import com.universitymanagement.assignment.repository.AssignmentRepository;
import com.universitymanagement.assignment.repository.SubmissionRepository;
import com.universitymanagement.attendance.repository.ClassSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Read-only by default; the write methods below each carry their own plain
 * {@code @Transactional}, which overrides this at the method level. Without
 * a session, every read here that touches a teacher's departments or
 * subjects (both lazy) threw LazyInitializationException the moment
 * open-in-view stopped papering over it — that includes /teachers/me,
 * which is why it was returning 500.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherServiceImpl implements TeacherService {

    /**
     * Employment statuses that stop a teacher using the platform.
     *
     * <p>Kept in step with ClassroomGradeGuard, which refuses the same set.
     * "on-leave" is absent from both: someone away for a term is still staff.
     */
    private static final java.util.Set<String> BLOCKED_STATUSES =
            java.util.Set.of("suspended", "inactive", "terminated");

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
    private final MinioService minioService;
    private final UserProfileWriter userProfileWriter;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ClassSessionRepository classSessionRepository;
    private final com.universitymanagement.grading.repository.CourseGradeRepository courseGradeRepository;
    private final com.universitymanagement.attendance.service.AttendanceSessionService attendanceSessionService;

    @Value("${keycloak.target-realm}")
    private String realm;

    @Override
    public Page<TeacherResponse> getAllTeachers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return teacherRepository.findAllLive(pageable).map(teacherMapper::toResponse);
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

        // Person-level extras that createUser() doesn't know about.
        User user = teacher.getUser();
        if (user != null) {
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.syncFullName();
            user.setNameKhmer(request.nameKhmer());
            user.setIdCardNumber(request.idCardNumber());
            user.setPlaceOfBirth(request.placeOfBirth());
            user.setCurrentAddress(request.currentAddress());
            user.setAddress(request.address() != null ? request.address() : request.currentAddress());
            userRepository.save(user);
        }

        if (request.teacherCode() != null && !request.teacherCode().isBlank()) {
            String code = request.teacherCode().trim();
            if (!code.equals(teacher.getTeacherCode()) && teacherRepository.existsByTeacherCode(code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Teacher code already in use: " + code);
            }
            teacher.setTeacherCode(code);
        }

        teacher.setSpecialization(request.specialization());
        if (request.departmentIds() != null && !request.departmentIds().isEmpty()) {
            teacher.setDepartments(resolveDepartments(request.departmentIds()));
        }

        teacher.setPosition(request.position());
        if (request.hireDate() != null) {
            teacher.setHireDate(request.hireDate());
        }
        if (request.employmentStatus() != null) {
            teacher.setEmploymentStatus(request.employmentStatus());

            // The guard already refuses a suspended teacher on the next
            // request. Mirroring it into Keycloak takes their tokens away too,
            // so the change survives a browser tab they already have open —
            // and putting the status back re-enables them.
            syncSignInWithStatus(teacher);
        }

        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public TeacherResponse updateTeacher(UUID teacherId, UpdateTeacherRequest request) {
        Teacher teacher = findTeacher(teacherId);

        // 1. Person-level fields (users table + Keycloak).
        User user = teacher.getUser();
        if (user != null) {
            userProfileWriter.apply(user, request);
        }

        if (request.teacherCode() != null && !request.teacherCode().isBlank()) {
            String code = request.teacherCode().trim();
            if (!code.equals(teacher.getTeacherCode()) && teacherRepository.existsByTeacherCode(code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Teacher code already in use: " + code);
            }
            teacher.setTeacherCode(code);
        }
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

            // The guard already refuses a suspended teacher on the next
            // request. Mirroring it into Keycloak takes their tokens away too,
            // so the change survives a browser tab they already have open —
            // and putting the status back re-enables them.
            syncSignInWithStatus(teacher);
        }

        return teacherMapper.toResponse(teacherRepository.save(teacher));
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    /**
     * Retires a teacher without destroying what they marked.
     *
     * <p>Deleting the row would take their grading, attendance registers and
     * assignment feedback with it — records the university has to keep, and
     * which other people's transcripts depend on. So the record is marked
     * retired and the sign-in account is disabled instead.
     */
    public void deleteTeacher(UUID teacherId) {
        Teacher teacher = findTeacher(teacherId);

        // Classrooms lose their lead teacher so the class is not left pointing
        // at somebody who has gone, and the next screen can prompt for a
        // replacement. Everything they graded stays exactly where it is.
        List<Classroom> classrooms = classroomRepository.findByTeacher_TeacherId(teacherId);
        if (classrooms != null && !classrooms.isEmpty()) {
            classrooms.forEach(c -> c.setTeacher(null));
            classroomRepository.saveAll(classrooms);
        }

        teacher.setIsDeleted(true);
        teacher.setEmploymentStatus("inactive");
        teacherRepository.save(teacher);

        disableSignIn(teacher.getUser());
    }

    @Override
    public Page<TeacherResponse> getWithdrawnTeachers(int page, int size) {
        return teacherRepository.findAllWithdrawn(PageRequest.of(page, size))
                .map(teacherMapper::toResponse);
    }

    @Override
    @Transactional
    public void restoreTeacher(UUID teacherId) {
        Teacher teacher = findTeacher(teacherId);

        teacher.setIsDeleted(false);
        teacher.setEmploymentStatus("active");
        teacherRepository.save(teacher);

        enableSignIn(teacher.getUser());
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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The record was restored but the sign-in account could not "
                            + "be re-enabled. Enable it in Keycloak.", e);
        }
    }

    /** Enables or disables sign-in to match the teacher's employment status. */
    private void syncSignInWithStatus(Teacher teacher) {
        String status = teacher.getEmploymentStatus() == null
                ? "" : teacher.getEmploymentStatus().trim().toLowerCase();

        if (BLOCKED_STATUSES.contains(status)) {
            disableSignIn(teacher.getUser());
        } else {
            enableSignIn(teacher.getUser());
        }
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
            // Loud on purpose: an account that can still sign in is exactly
            // what this method exists to prevent, so a half-done removal must
            // not look like a finished one.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The record was withdrawn but the sign-in account could not "
                            + "be disabled. Disable it in Keycloak before relying on this.", e);
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
    @Transactional
    public TeacherDetailResponse findTeacherByUserId(String id) {
        UserRepresentation kcUser = requireKeycloakUser(id);
        List<String> roles = fetchRealmRoles(id);

        User user = userRepository.findByKeycloakId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local DB: " + id));

        Teacher teacher = teacherRepository.findByUserIdWithDepartments(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found for user: " + id));

        // getAssetPreviewUrl, not getPreviewUrl: the avatar was written to the
        // assets bucket by uploadMyAvatar()'s call to minioService.uploadAsset(),
        // but getPreviewUrl signs a URL against the *lessons* bucket instead. The
        // object doesn't exist there, so the signed URL 404s in the browser even
        // though the upload itself succeeded. TeacherMapper.toResponse already
        // gets this right for every other teacher-listing endpoint — this was the
        // one path that didn't match it.
        String avatarUrl = user.getAvatarObjectName() != null
                ? minioService.getAssetPreviewUrl(user.getAvatarObjectName())
                : null;

        return new TeacherDetailResponse(
                kcUser.getId(),
                teacher.getTeacherId().toString(),
                kcUser.getUsername(),
                kcUser.getEmail(),
                user.resolvedFirstName() != null ? user.resolvedFirstName() : kcUser.getFirstName(),
                user.resolvedLastName() != null ? user.resolvedLastName() : kcUser.getLastName(),
                Boolean.TRUE.equals(kcUser.isEnabled()),
                roles,
                teacher.getTeacherCode(),
                teacher.getDepartments().stream()
                        .map(teacherMapper::toDepartmentResponse)
                        .toList(),
                teacher.getPosition(),
                teacher.getSpecialization(),
                teacher.getHireDate(),
                teacher.getEmploymentStatus(),
                avatarUrl
        );
    }

    @Override
    public TeacherDashboardSummaryResponse getMyDashboardSummary(String userId) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local DB: " + userId));
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found for user: " + userId));

        List<UUID> classroomIds = classroomRepository
                .findByTeacher_TeacherIdAndIsDeletedFalse(teacher.getTeacherId())
                .stream()
                .map(Classroom::getClassroomId)
                .toList();

        if (classroomIds.isEmpty()) {
            return new TeacherDashboardSummaryResponse(0, 0, 0, 0, 0, null, null);
        }

        long totalStudents = classroomStudentRepository.countDistinctStudentsByClassroomIds(classroomIds);
        long courseMaterials = lessonRepository.countByClassroomIds(classroomIds)
                + assignmentRepository.countByClassroomIds(classroomIds);
        long toGrade = submissionRepository.countUngradedByClassroomIds(classroomIds);
        long attendanceToday = classSessionRepository.countUntakenTodayByClassroomIds(classroomIds, java.time.LocalDate.now());

        Double avgAttendancePercent = classroomIds.stream()
                .flatMap(id -> attendanceSessionService.getSummary(id).stream())
                .map(com.universitymanagement.attendance.dto.response.AttendanceSummaryResponse::attendancePercent)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream().boxed().findFirst()
                .orElse(null);

        Double avgPerformancePercent = courseGradeRepository.findByClassroomsWithCourse(classroomIds).stream()
                .map(com.universitymanagement.grading.entity.CourseGrade::getScorePercent)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream().boxed().findFirst()
                .orElse(null);

        return new TeacherDashboardSummaryResponse(
                classroomIds.size(), totalStudents, courseMaterials, toGrade, attendanceToday,
                avgAttendancePercent, avgPerformancePercent);
    }

    @Override
    public List<com.universitymanagement.teacher.dto.response.StudentMetricsResponse> getMyStudentMetrics(String userId) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local DB: " + userId));
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found for user: " + userId));

        List<UUID> classroomIds = classroomRepository
                .findByTeacher_TeacherIdAndIsDeletedFalse(teacher.getTeacherId())
                .stream()
                .map(Classroom::getClassroomId)
                .toList();

        if (classroomIds.isEmpty()) {
            return List.of();
        }

        java.util.Map<UUID, List<Double>> attendanceByStudent = classroomIds.stream()
                .flatMap(id -> attendanceSessionService.getSummary(id).stream())
                .filter(s -> s.attendancePercent() != null)
                .collect(Collectors.groupingBy(
                        com.universitymanagement.attendance.dto.response.AttendanceSummaryResponse::studentId,
                        Collectors.mapping(
                                com.universitymanagement.attendance.dto.response.AttendanceSummaryResponse::attendancePercent,
                                Collectors.toList())));

        java.util.Map<UUID, List<Double>> performanceByStudent = courseGradeRepository.findByClassroomsWithCourse(classroomIds).stream()
                .filter(g -> g.getScorePercent() != null)
                .collect(Collectors.groupingBy(
                        g -> g.getStudent().getStudentId(),
                        Collectors.mapping(com.universitymanagement.grading.entity.CourseGrade::getScorePercent, Collectors.toList())));

        Set<UUID> allStudentIds = new java.util.HashSet<>();
        allStudentIds.addAll(attendanceByStudent.keySet());
        allStudentIds.addAll(performanceByStudent.keySet());
        classroomStudentRepository.findByClassroom_ClassroomIdIn(classroomIds)
                .forEach(cs -> allStudentIds.add(cs.getStudent().getStudentId()));

        return allStudentIds.stream()
                .map(studentId -> new com.universitymanagement.teacher.dto.response.StudentMetricsResponse(
                        studentId,
                        average(attendanceByStudent.get(studentId)),
                        average(performanceByStudent.get(studentId))))
                .toList();
    }

    private Double average(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    @Override
    @Transactional
    public TeacherDetailResponse uploadMyAvatar(org.springframework.web.multipart.MultipartFile file) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User user = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found in local DB"));
        String objectName = minioService.uploadAsset(file);
        user.setAvatarObjectName(objectName);
        userRepository.save(user);
        return findTeacherByUserId(user.getKeycloakId());
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

    @Override
    @Transactional
    public void resetPassword(UUID teacherId, AdminResetPasswordRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Teacher not found: " + teacherId));

        User user = teacher.getUser();
        if (user == null || user.getKeycloakId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This teacher has no sign-in account to reset.");
        }

        userManageService.resetPassword(user.getKeycloakId(), request);
    }
}
