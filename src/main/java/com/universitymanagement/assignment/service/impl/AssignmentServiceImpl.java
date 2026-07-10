package com.universitymanagement.assignment.service.impl;

import com.universitymanagement.assignment.dto.request.AssignmentRequest;
import com.universitymanagement.assignment.dto.request.GradeSubmissionRequest;
import com.universitymanagement.assignment.dto.response.AssignmentResponse;
import com.universitymanagement.assignment.dto.response.SubmissionResponse;
import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.assignment.entity.Submission;
import com.universitymanagement.assignment.entity.SubmissionStatus;
import com.universitymanagement.assignment.repository.AssignmentRepository;
import com.universitymanagement.assignment.repository.SubmissionRepository;
import com.universitymanagement.assignment.service.AssignmentService;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(UUID classroomId, AssignmentRequest request, MultipartFile file) {
        Classroom classroom = findClassroom(classroomId);
        Teacher teacher = requireTeacherOwnsClassroom(classroom);

        Assignment assignment = new Assignment();
        assignment.setClassroom(classroom);
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore());
        assignment.setWeight(request.weight());
        assignment.setCreatedByTeacher(teacher);

        if (file != null && !file.isEmpty()) {
            assignment.setFileObjectName(minioService.uploadLessonFile(file));
            assignment.setFileOriginalName(file.getOriginalFilename());
        }

        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId) {
        Assignment assignment = findAssignment(assignmentId);
        requireTeacherOwnsClassroomOrAdmin(assignment.getClassroom());

        return submissionRepository
                .findByAssignment_AssignmentIdOrderBySubmittedAtDesc(assignmentId)
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Override
    @Transactional
    public SubmissionResponse gradeSubmission(UUID submissionId, GradeSubmissionRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        Teacher teacher = requireTeacherOwnsClassroom(submission.getAssignment().getClassroom());

        Double maxScore = submission.getAssignment().getMaxScore();
        if (maxScore != null && request.score() > maxScore) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Score cannot exceed max score of " + maxScore);
        }

        submission.setScore(request.score());
        submission.setFeedback(request.feedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedBy(teacher);
        submission.setGradedAt(LocalDateTime.now());

        return toSubmissionResponse(submissionRepository.save(submission));
    }


    @Override
    public List<AssignmentResponse> getAssignmentsByClassroom(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        requireMemberOrAdmin(classroom);

        return assignmentRepository
                .findByClassroom_ClassroomIdAndIsDeletedFalseOrderByDueDateAsc(classroomId)
                .stream()
                .map(this::toAssignmentResponse)
                .toList();
    }


    @Override
    @Transactional
    public SubmissionResponse submitAssignment(UUID assignmentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission file is required");
        }

        Assignment assignment = findAssignment(assignmentId);
        Student student = requireEnrolledStudent(assignment.getClassroom());

        Submission submission = submissionRepository
                .findByAssignment_AssignmentIdAndStudent_StudentId(assignmentId, student.getStudentId())
                .orElseGet(() -> {
                    Submission s = new Submission();
                    s.setAssignment(assignment);
                    s.setStudent(student);
                    return s;
                });

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This submission has already been graded");
        }

        LocalDateTime now = LocalDateTime.now();
        submission.setFileObjectName(minioService.uploadLessonFile(file));
        submission.setFileOriginalName(file.getOriginalFilename());
        submission.setSubmittedAt(now);
        submission.setStatus(now.isAfter(assignment.getDueDate())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED);

        return toSubmissionResponse(submissionRepository.save(submission));
    }


    private AssignmentResponse toAssignmentResponse(Assignment a) {
        String fileUrl = a.getFileObjectName() != null
                ? minioService.getPreviewUrl(a.getFileObjectName())
                : null;

        return new AssignmentResponse(
                a.getAssignmentId(),
                a.getClassroom().getClassroomId(),
                a.getTitle(),
                a.getDescription(),
                a.getDueDate(),
                a.getMaxScore(),
                a.getWeight(),
                a.getFileOriginalName(),
                fileUrl,
                a.getCreatedAt(),
                a.getCreatedBy()
        );
    }

    private SubmissionResponse toSubmissionResponse(Submission s) {
        String fileUrl = s.getFileObjectName() != null
                ? minioService.getPreviewUrl(s.getFileObjectName())
                : null;

        return new SubmissionResponse(
                s.getSubmissionId(),
                s.getAssignment().getAssignmentId(),
                s.getStudent().getStudentId(),
                s.getStudent().getStudentCode(),
                s.getStudent().getUser().getFullName(),
                s.getFileOriginalName(),
                fileUrl,
                s.getSubmittedAt(),
                s.getStatus(),
                s.getScore(),
                s.getFeedback(),
                s.getGradedAt()
        );
    }


    private Assignment findAssignment(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private Classroom findClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found"));
    }

    private Teacher requireTeacherOwnsClassroom(Classroom classroom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = getCurrentUser(auth);
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Teacher profile not found for current user"));

        boolean owns = classroom.getTeacher() != null
                && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId());
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not the teacher of this classroom");
        }
        return teacher;
    }

    private void requireTeacherOwnsClassroomOrAdmin(Classroom classroom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(auth, "ADMIN")) {
            return;
        }
        requireTeacherOwnsClassroom(classroom);
    }

    private Student requireEnrolledStudent(Classroom classroom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = getCurrentUser(auth);
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Student profile not found for current user"));

        boolean enrolled = classroomStudentRepository
                .existsByClassroom_ClassroomIdAndStudent_StudentId(
                        classroom.getClassroomId(), student.getStudentId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not enrolled in this classroom");
        }
        return student;
    }

    private void requireMemberOrAdmin(Classroom classroom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(auth, "ADMIN")) {
            return;
        }
        User user = getCurrentUser(auth);

        if (hasRole(auth, "TEACHER")) {
            Teacher teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
            if (teacher != null && classroom.getTeacher() != null
                    && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId())) {
                return;
            }
        }

        if (hasRole(auth, "STUDENT")) {
            Student student = studentRepository.findByUserId(user.getId()).orElse(null);
            if (student != null && classroomStudentRepository
                    .existsByClassroom_ClassroomIdAndStudent_StudentId(
                            classroom.getClassroomId(), student.getStudentId())) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not a member of this classroom");
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
}
