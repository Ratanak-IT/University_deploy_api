package com.universitymanagement.assignment.service.impl;

import com.universitymanagement.assignment.dto.request.CreatePrivateCommentRequest;
import com.universitymanagement.assignment.dto.response.PrivateCommentResponse;
import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.assignment.entity.PrivateComment;
import com.universitymanagement.assignment.repository.AssignmentRepository;
import com.universitymanagement.assignment.repository.PrivateCommentRepository;
import com.universitymanagement.assignment.service.PrivateCommentService;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.notification.service.NotificationService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.student.security.StudentAccessGuard;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Backs the "Private comments" panel on an assignment: a thread between one
 * student and the teacher(s) of that classroom, closed to everyone else —
 * classmates included, which is what separates this from the public class
 * comments the same page also shows.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PrivateCommentServiceImpl implements PrivateCommentService {

    private final PrivateCommentRepository commentRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final StudentAccessGuard studentAccessGuard;
    private final MinioService minioService;
    private final NotificationService notificationService;

    @Override
    public List<PrivateCommentResponse> getMyThread(UUID assignmentId) {
        Student student = studentAccessGuard.getCurrentStudent();
        Assignment assignment = findAssignment(assignmentId);
        requireEnrolled(assignment.getClassroom(), student);
        return toResponses(assignment, student);
    }

    @Override
    @Transactional
    public PrivateCommentResponse postToMyThread(UUID assignmentId, CreatePrivateCommentRequest request) {
        Student student = studentAccessGuard.getCurrentStudent();
        Assignment assignment = findAssignment(assignmentId);
        requireEnrolled(assignment.getClassroom(), student);

        PrivateComment saved = save(assignment, student, student.getUser(), request.body());

        // Best-effort: tell the classroom's teacher a student wrote in. A
        // notification failure must not roll back the message itself.
        Classroom classroom = assignment.getClassroom();
        if (classroom.getTeacher() != null && classroom.getTeacher().getUser() != null) {
            try {
                notificationService.createNotification(
                        classroom.getTeacher().getUser().getId(),
                        "New private comment",
                        displayName(student.getUser()) + " wrote about " + assignment.getTitle() + ".",
                        "COMMENT_REPLY",
                        classroom.getClassName(),
                        displayName(student.getUser()),
                        "/dashboard/teacher/assignments/" + assignment.getAssignmentId(),
                        "ASSIGNMENT",
                        assignment.getAssignmentId()
                );
            } catch (Exception e) {
                log.warn("Private-comment notification failed for teacher: {}", e.getMessage());
            }
        }

        return toResponse(saved, student);
    }

    @Override
    public List<PrivateCommentResponse> getStudentThread(UUID assignmentId, UUID studentId) {
        Assignment assignment = findAssignment(assignmentId);
        Student student = findStudent(studentId);
        requireTeacherOwnsClassroomOrAdmin(assignment.getClassroom());
        return toResponses(assignment, student);
    }

    @Override
    @Transactional
    public PrivateCommentResponse postToStudentThread(UUID assignmentId, UUID studentId,
                                                       CreatePrivateCommentRequest request) {
        Assignment assignment = findAssignment(assignmentId);
        Student student = findStudent(studentId);
        requireTeacherOwnsClassroomOrAdmin(assignment.getClassroom());

        User author = currentUser();
        PrivateComment saved = save(assignment, student, author, request.body());

        // Best-effort: tell the student their teacher wrote back.
        if (student.getUser() != null) {
            try {
                notificationService.createNotification(
                        student.getUser().getId(),
                        "New private comment",
                        displayName(author) + " wrote about " + assignment.getTitle() + ".",
                        "COMMENT_REPLY",
                        assignment.getClassroom().getClassName(),
                        displayName(author),
                        "/dashboard/student/courses/assignment?assignmentId=" + assignment.getAssignmentId(),
                        "ASSIGNMENT",
                        assignment.getAssignmentId()
                );
            } catch (Exception e) {
                log.warn("Private-comment notification failed for student {}: {}",
                        student.getStudentId(), e.getMessage());
            }
        }

        return toResponse(saved, student);
    }

    // ---- helpers ----

    private PrivateComment save(Assignment assignment, Student student, User author, String body) {
        PrivateComment comment = new PrivateComment();
        comment.setAssignment(assignment);
        comment.setStudent(student);
        comment.setAuthor(author);
        comment.setBody(body.trim());
        return commentRepository.save(comment);
    }

    private List<PrivateCommentResponse> toResponses(Assignment assignment, Student student) {
        return commentRepository
                .findThread(assignment.getAssignmentId(), student.getStudentId())
                .stream()
                .map(c -> toResponse(c, student))
                .toList();
    }

    /** `student` is the thread owner — comparing the author to it is what tells a STUDENT message from a TEACHER one. */
    private PrivateCommentResponse toResponse(PrivateComment c, Student student) {
        User author = c.getAuthor();
        boolean isStudentAuthor = student.getUser() != null
                && author.getId().equals(student.getUser().getId());

        return new PrivateCommentResponse(
                c.getCommentId(),
                c.getAssignment().getAssignmentId(),
                student.getStudentId(),
                author.getId(),
                displayName(author),
                isStudentAuthor ? "STUDENT" : "TEACHER",
                avatarUrl(author),
                c.getBody(),
                c.getCreatedAt()
        );
    }

    private String displayName(User u) {
        if (u.getFullName() != null && !u.getFullName().isBlank()) {
            return u.getFullName();
        }
        return u.getEmail();
    }

    /** Never let a MinIO hiccup break the whole thread — the avatar is decoration. */
    private String avatarUrl(User u) {
        if (u.getAvatarObjectName() == null) {
            return null;
        }
        try {
            return minioService.getAssetPreviewUrl(u.getAvatarObjectName());
        } catch (Exception e) {
            return null;
        }
    }

    private void requireEnrolled(Classroom classroom, Student student) {
        boolean enrolled = classroomStudentRepository
                .existsByClassroom_ClassroomIdAndStudent_StudentId(
                        classroom.getClassroomId(), student.getStudentId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not enrolled in this classroom");
        }
    }

    private void requireTeacherOwnsClassroomOrAdmin(Classroom classroom) {
        if (hasRole("ADMIN")) {
            return;
        }
        User user = currentUser();
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Teacher profile not found for current user"));

        boolean owns = classroom.getTeacher() != null
                && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId());
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not the teacher of this classroom");
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found in local DB"));
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    private Assignment findAssignment(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Assignment not found: " + assignmentId));
    }

    private Student findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found: " + studentId));
    }
}
