package com.universitymanagement.classroom.service.impl;

import com.universitymanagement.classroom.dto.request.AddStudentsRequest;
import com.universitymanagement.classroom.dto.request.AssignTeacherRequest;
import com.universitymanagement.classroom.dto.request.ClassroomCreateRequest;
import com.universitymanagement.classroom.dto.request.ClassroomUpdateRequest;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.dto.response.ClassroomStudentResponse;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.mapper.ClassroomMapper;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.classroom.service.ClassroomService;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.program.repository.ProgramRepository;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.repository.SubjectRepository;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomMapper classroomMapper;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I


    @Override
    public Page<ClassroomResponse> getAllClassrooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return classroomRepository.findByIsDeletedFalse(pageable)
                .map(classroomMapper::toResponse);
    }

    @Override
    @Transactional
    public ClassroomResponse createClassroom(ClassroomCreateRequest request) {
        Teacher teacher = findTeacher(request.teacherId());
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        Classroom classroom = classroomMapper.toEntity(request);
        classroom.setTeacher(teacher);
        classroom.setSubject(subject);
        classroom.setProgram(program);
        classroom.setClassCode(generateClassCode(subject.getSubjectCode()));
        classroom.setInviteCode(generateInviteCode());
        classroom.setIsDeleted(false);

        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public ClassroomResponse updateClassroom(UUID classroomId, ClassroomUpdateRequest request) {
        Classroom classroom = findClassroom(classroomId);

        classroomMapper.updateEntity(request, classroom);

        // relations updated only when the id is provided
        if (request.teacherId() != null) {
            classroom.setTeacher(findTeacher(request.teacherId()));
        }
        if (request.subjectId() != null) {
            classroom.setSubject(subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found")));
        }
        if (request.programId() != null) {
            classroom.setProgram(programRepository.findById(request.programId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found")));
        }

        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Override
    @Transactional
    public ClassroomResponse assignTeacher(UUID classroomId, AssignTeacherRequest request) {
        Classroom classroom = findClassroom(classroomId);
        classroom.setTeacher(findTeacher(request.teacherId()));
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
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Student not found: " + studentId));

            boolean alreadyEnrolled = classroomStudentRepository
                    .existsByClassroom_ClassroomIdAndStudent_StudentId(classroomId, studentId);
            if (alreadyEnrolled) {
                continue; // skip duplicates instead of failing the whole batch
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student is not enrolled in this classroom"));
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
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Teacher profile not found for current user"));
            return classroomRepository
                    .findByTeacher_TeacherIdAndIsDeletedFalse(teacher.getTeacherId())
                    .stream()
                    .map(classroomMapper::toResponse)
                    .toList();
        }

        if (hasRole(authentication, "STUDENT")) {
            Student student = studentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Student profile not found for current user"));
            return classroomStudentRepository
                    .findByStudent_StudentId(student.getStudentId())
                    .stream()
                    .map(ClassroomStudent::getClassroom)
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .map(classroomMapper::toResponse)
                    .toList();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only teachers or students have their own classrooms");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private Classroom findClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found"));
    }

    private Teacher findTeacher(UUID teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
    }

    /**
     * OWASP A01 (Broken Access Control) protection:
     * a TEACHER must only see classrooms that belong to them.
     * ADMIN can see everything.
     */
    private void checkTeacherOwnsClassroomIfTeacher(Classroom classroom) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(authentication, "ADMIN")) {
            return;
        }
        if (hasRole(authentication, "TEACHER")) {
            User user = getCurrentUser(authentication);
            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Teacher profile not found for current user"));
            boolean owns = classroom.getTeacher() != null
                    && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId());
            if (!owns) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not the teacher of this classroom");
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

    private String generateClassCode(String subjectCode) {
        // e.g. WEB101-2026-A7K3 -> unique, readable, retry until free
        String code;
        do {
            code = "%s-%d-%s".formatted(subjectCode, Year.now().getValue(), randomCode(4));
        } while (classroomRepository.existsByClassCode(code));
        return code;
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
