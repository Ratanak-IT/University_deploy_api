package com.universitymanagement.classroom.service.impl;

import com.universitymanagement.classroom.dto.ClassroomRole;
import com.universitymanagement.classroom.dto.MemberStatus;
import com.universitymanagement.classroom.dto.request.AddStudentsRequest;
import com.universitymanagement.classroom.dto.request.AssignTeacherRequest;
import com.universitymanagement.classroom.dto.request.ClassroomCreateRequest;
import com.universitymanagement.classroom.dto.request.ClassroomUpdateRequest;
import com.universitymanagement.classroom.dto.response.ClassroomMemberResponse;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.dto.response.ClassroomStudentResponse;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomMember;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.exception.ClassroomAccessDeniedException;
import com.universitymanagement.classroom.exception.ClassroomNotFoundException;
import com.universitymanagement.classroom.exception.StudentNotEnrolledException;
import com.universitymanagement.classroom.exception.TeacherAlreadyAssignedException;
import com.universitymanagement.classroom.exception.TeacherNotInClassroomException;
import com.universitymanagement.classroom.mapper.ClassroomMapper;
import com.universitymanagement.classroom.repository.ClassroomMemberRepository;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.classroom.service.ClassroomService;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.program.exception.ProgramNotFoundException;
import com.universitymanagement.program.repository.ProgramRepository;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.exception.StudentNotFoundException;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.exception.SubjectNotFoundException;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.exception.TeacherNotFoundException;
import com.universitymanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final ClassroomMapper classroomMapper;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Override
    public Page<ClassroomResponse> getAllClassrooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return classroomRepository.findByIsDeletedFalse(pageable)
                .map(classroomMapper::toResponse);
    }

    @Override
    public Page<ClassroomResponse> searchClassrooms(
            String keyword, UUID programId, Integer yearLevel, Integer semester, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return classroomRepository
                .search(normalizedKeyword, programId, yearLevel, semester, pageable)
                .map(classroomMapper::toResponse);
    }


    @Override
    @Transactional
    public ClassroomResponse createClassroom(ClassroomCreateRequest request) {
        Teacher teacher = findTeacher(request.teacherId());
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ProgramNotFoundException(request.programId()));

        Classroom classroom = classroomMapper.toEntity(request);
        classroom.setTeacher(teacher);
        classroom.setSubject(subject);
        classroom.setProgram(program);
        classroom.setClassCode(generateClassCode());
        classroom.setInviteCode(generateInviteCode());
        classroom.setIsDeleted(false);

        Classroom saved = classroomRepository.save(classroom);
        upsertTeacherMember(saved, teacher);

        return classroomMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ClassroomResponse updateClassroom(UUID classroomId, ClassroomUpdateRequest request) {
        Classroom classroom = findClassroom(classroomId);

        classroomMapper.updateEntity(request, classroom);

        if (request.teacherId() != null) {
            Teacher teacher = findTeacher(request.teacherId());
            classroom.setTeacher(teacher);
            upsertTeacherMember(classroom, teacher);
        }
        if (request.subjectId() != null) {
            classroom.setSubject(subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new SubjectNotFoundException(request.subjectId())));
        }
        if (request.programId() != null) {
            classroom.setProgram(programRepository.findById(request.programId())
                    .orElseThrow(() -> new ProgramNotFoundException(request.programId())));
        }

        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public ClassroomMemberResponse addTeacherToClassroom(UUID classroomId, AssignTeacherRequest request) {
        Classroom classroom = findClassroom(classroomId);
        Teacher teacher = findTeacher(request.teacherId());
        User user = requireTeacherUser(teacher);

        boolean alreadyAssigned = classroomMemberRepository
                .existsByClassroom_ClassroomIdAndUser_IdAndRoleAndStatus(
                        classroomId, user.getId(), ClassroomRole.TEACHER, MemberStatus.ACTIVE);
        if (alreadyAssigned) {
            throw new TeacherAlreadyAssignedException(teacher.getTeacherId(), classroomId);
        }

        ClassroomMember member = upsertTeacherMember(classroom, teacher);

        if (classroom.getTeacher() == null) {
            classroom.setTeacher(teacher);
            classroomRepository.save(classroom);
        }

        return toMemberResponse(member);
    }

    @Override
    public List<ClassroomMemberResponse> getTeachersInClassroom(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        checkTeacherOwnsClassroomIfTeacher(classroom);

        return classroomMemberRepository
                .findByClassroom_ClassroomIdAndRoleAndStatus(
                        classroomId, ClassroomRole.TEACHER, MemberStatus.ACTIVE)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeTeacherFromClassroom(UUID classroomId, UUID teacherId) {
        Classroom classroom = findClassroom(classroomId);
        Teacher teacher = findTeacher(teacherId);
        User user = requireTeacherUser(teacher);

        ClassroomMember member = classroomMemberRepository
                .findByClassroom_ClassroomIdAndUser_Id(classroomId, user.getId())
                .filter(m -> m.getRole() == ClassroomRole.TEACHER
                        && m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new TeacherNotInClassroomException(teacherId, classroomId));

        member.setStatus(MemberStatus.REMOVED);
        classroomMemberRepository.save(member);

        boolean isLead = classroom.getTeacher() != null
                && classroom.getTeacher().getTeacherId().equals(teacherId);
        if (isLead) {
            classroom.setTeacher(findAnyRemainingTeacher(classroomId));
            classroomRepository.save(classroom);
        }
    }

    @Override
    @Transactional
    public ClassroomResponse setLeadTeacher(UUID classroomId, AssignTeacherRequest request) {
        Classroom classroom = findClassroom(classroomId);
        Teacher teacher = findTeacher(request.teacherId());
        User user = requireTeacherUser(teacher);

        boolean isMember = classroomMemberRepository
                .existsByClassroom_ClassroomIdAndUser_IdAndRoleAndStatus(
                        classroomId, user.getId(), ClassroomRole.TEACHER, MemberStatus.ACTIVE);
        if (!isMember) {
            throw new TeacherNotInClassroomException(teacher.getTeacherId(), classroomId);
        }

        classroom.setTeacher(teacher);
        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public void softDelete(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        classroom.setIsDeleted(true);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void addStudentsToClassroom(UUID classroomId, AddStudentsRequest request) {
        Classroom classroom = findClassroom(classroomId);

        for (UUID studentId : request.studentIds()) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new StudentNotFoundException(studentId));

            boolean alreadyEnrolled = classroomStudentRepository
                    .existsByClassroom_ClassroomIdAndStudent_StudentId(classroomId, studentId);
            if (alreadyEnrolled) {
                continue;
            }

            ClassroomStudent enrollment = new ClassroomStudent();
            enrollment.setClassroom(classroom);
            enrollment.setStudent(student);
            classroomStudentRepository.save(enrollment);
        }
    }

    @Override
    @Transactional
    public void removeStudentFromClassroom(UUID classroomId, UUID studentId) {
        ClassroomStudent enrollment = classroomStudentRepository
                .findByClassroom_ClassroomIdAndStudent_StudentId(classroomId, studentId)
                .orElseThrow(() -> new StudentNotEnrolledException(
                        "Student is not enrolled in this classroom"));
        classroomStudentRepository.delete(enrollment);
    }

    @Override
    public ClassroomResponse getClassroomById(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        checkTeacherOwnsClassroomIfTeacher(classroom);
        return classroomMapper.toResponse(classroom);
    }

    @Override
    public List<ClassroomStudentResponse> getStudentsInClassroom(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        checkTeacherOwnsClassroomIfTeacher(classroom);

        return classroomStudentRepository.findByClassroom_ClassroomId(classroomId)
                .stream()
                .map(classroomMapper::toStudentResponse)
                .toList();
    }

    @Override
    public List<ClassroomResponse> getMyClassrooms() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getCurrentUser(authentication);

        if (hasRole(authentication, "TEACHER")) {
            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new TeacherNotFoundException(
                            "Teacher profile not found for current user"));

            Map<UUID, Classroom> merged = new LinkedHashMap<>();

            classroomRepository
                    .findByTeacher_TeacherIdAndIsDeletedFalse(teacher.getTeacherId())
                    .forEach(c -> merged.put(c.getClassroomId(), c));

            classroomMemberRepository
                    .findByUser_IdAndRoleAndStatus(
                            user.getId(), ClassroomRole.TEACHER, MemberStatus.ACTIVE)
                    .stream()
                    .map(ClassroomMember::getClassroom)
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .forEach(c -> merged.put(c.getClassroomId(), c));

            return merged.values().stream()
                    .map(classroomMapper::toResponse)
                    .toList();
        }

        if (hasRole(authentication, "STUDENT")) {
            Student student = studentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new StudentNotFoundException(
                            "Student profile not found for current user"));
            return classroomStudentRepository
                    .findByStudent_StudentId(student.getStudentId())
                    .stream()
                    .map(ClassroomStudent::getClassroom)
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .map(classroomMapper::toResponse)
                    .toList();
        }

        throw new ClassroomAccessDeniedException(
                "Only teachers or students have their own classrooms");
    }


    private ClassroomMember upsertTeacherMember(Classroom classroom, Teacher teacher) {
        User user = requireTeacherUser(teacher);

        ClassroomMember member = classroomMemberRepository
                .findByClassroom_ClassroomIdAndUser_Id(classroom.getClassroomId(), user.getId())
                .orElseGet(() -> {
                    ClassroomMember m = new ClassroomMember();
                    m.setClassroom(classroom);
                    m.setUser(user);
                    return m;
                });

        member.setRole(ClassroomRole.TEACHER);
        member.setStatus(MemberStatus.ACTIVE);
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }
        return classroomMemberRepository.save(member);
    }

    private Teacher findAnyRemainingTeacher(UUID classroomId) {
        List<ClassroomMember> remaining = classroomMemberRepository
                .findByClassroom_ClassroomIdAndRoleAndStatus(
                        classroomId, ClassroomRole.TEACHER, MemberStatus.ACTIVE);
        if (remaining.isEmpty()) {
            return null;
        }
        return teacherRepository.findByUserId(remaining.get(0).getUser().getId()).orElse(null);
    }

    private User requireTeacherUser(Teacher teacher) {
        if (teacher.getUser() == null) {
            throw new UserNotFoundException();
        }
        return teacher.getUser();
    }

    private ClassroomMemberResponse toMemberResponse(ClassroomMember member) {
        User user = member.getUser();
        return new ClassroomMemberResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                member.getRole(),
                member.getJoinedAt(),
                member.getStatus()
        );
    }

    private Classroom findClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ClassroomNotFoundException(classroomId));
    }

    private Teacher findTeacher(UUID teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
    }

    private void checkTeacherOwnsClassroomIfTeacher(Classroom classroom) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(authentication, "ADMIN")) {
            return;
        }
        if (hasRole(authentication, "TEACHER")) {
            User user = getCurrentUser(authentication);
            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new TeacherNotFoundException(
                            "Teacher profile not found for current user"));

            boolean isLead = classroom.getTeacher() != null
                    && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId());

            boolean isMember = classroomMemberRepository
                    .existsByClassroom_ClassroomIdAndUser_IdAndRoleAndStatus(
                            classroom.getClassroomId(), user.getId(),
                            ClassroomRole.TEACHER, MemberStatus.ACTIVE);

            if (!isLead && !isMember) {
                throw new ClassroomAccessDeniedException(
                        "You are not a teacher of this classroom");
            }
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserNotFoundException();
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(UserNotFoundException::new);
    }

    private String generateClassCode() {
        String classCode;
        boolean isDuplicate;
        do {
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            String randomPart = uuid.substring(0, 6);
            classCode = "CS-" + randomPart;
            isDuplicate = classroomRepository.existsByClassCode(classCode);
        } while (isDuplicate);
        return classCode;
    }

    private String generateInviteCode() {
        String code;
        do {
            code = randomCode(8);
        } while (classroomRepository.existsByInviteCode(code));
        return code;
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(INVITE_CHARS.charAt(RANDOM.nextInt(INVITE_CHARS.length())));
        }
        return sb.toString();
    }
}
