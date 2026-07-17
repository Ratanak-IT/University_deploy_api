package com.universitymanagement.assignment.service.impl;

import com.universitymanagement.assignment.dto.request.AssignmentRequest;
import com.universitymanagement.assignment.dto.request.GradeSubmissionRequest;
import com.universitymanagement.assignment.dto.response.AssignmentResponse;
import com.universitymanagement.assignment.dto.response.FileResponse;
import com.universitymanagement.assignment.dto.response.SubmissionResponse;
import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.assignment.entity.AssignmentFile;
import com.universitymanagement.assignment.entity.Submission;
import com.universitymanagement.assignment.entity.SubmissionFile;
import com.universitymanagement.assignment.entity.SubmissionStatus;
import com.universitymanagement.assignment.exception.ClassroomHasNoTeacherException;
import com.universitymanagement.assignment.exception.NotClassroomTeacherException;
import com.universitymanagement.assignment.exception.TeacherProfileNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public AssignmentResponse createAssignment(UUID classroomId, AssignmentRequest request, List<MultipartFile> files) {
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

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                AssignmentFile af = new AssignmentFile();
                af.setAssignment(assignment);
                af.setFileObjectName(minioService.uploadLessonFile(file));
                af.setFileOriginalName(file.getOriginalFilename());
                assignment.getFiles().add(af);
            }
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
    public SubmissionResponse submitAssignment(UUID assignmentId, List<MultipartFile> files) {
        boolean hasFile = files != null && files.stream()
                .anyMatch(f -> f != null && !f.isEmpty());
        if (!hasFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one submission file is required");
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

        submission.getFiles().clear();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            SubmissionFile sf = new SubmissionFile();
            sf.setSubmission(submission);
            sf.setFileObjectName(minioService.uploadLessonFile(file));
            sf.setFileOriginalName(file.getOriginalFilename());
            submission.getFiles().add(sf);
        }

        LocalDateTime now = LocalDateTime.now();
        submission.setSubmittedAt(now);
        submission.setStatus(now.isAfter(assignment.getDueDate())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED);

        return toSubmissionResponse(submissionRepository.save(submission));
    }


    private AssignmentResponse toAssignmentResponse(Assignment a) {
        List<FileResponse> files = a.getFiles().stream()
                .map(f -> new FileResponse(
                        f.getFileId(),
                        f.getFileOriginalName(),
                        minioService.getPreviewUrl(f.getFileObjectName())
                ))
                .toList();

        return new AssignmentResponse(
                a.getAssignmentId(),
                a.getClassroom().getClassroomId(),
                a.getTitle(),
                a.getDescription(),
                a.getDueDate(),
                a.getMaxScore(),
                a.getWeight(),
                files,
                a.getCreatedAt(),
                a.getCreatedBy()
        );
    }

    private SubmissionResponse toSubmissionResponse(Submission s) {
        List<FileResponse> files = s.getFiles().stream()
                .map(f -> new FileResponse(
                        f.getFileId(),
                        f.getFileOriginalName(),
                        minioService.getPreviewUrl(f.getFileObjectName())
                ))
                .toList();

        return new SubmissionResponse(
                s.getSubmissionId(),
                s.getAssignment().getAssignmentId(),
                s.getStudent().getStudentId(),
                s.getStudent().getStudentCode(),
                s.getStudent().getUser().getFullName(),
                files,
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

        if (hasRole(auth, "ADMIN")) {
            if (classroom.getTeacher() == null) {
                throw new ClassroomHasNoTeacherException(classroom.getClassroomId());
            }
            return classroom.getTeacher();
        }

        User user = getCurrentUser(auth);
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(TeacherProfileNotFoundException::new);

        boolean owns = classroom.getTeacher() != null
                && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId());
        if (!owns) {
            throw new NotClassroomTeacherException(classroom.getClassroomId());
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

    @Override
    public Page<AssignmentResponse> getAllAssignments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return assignmentRepository.findAll(pageable).map(this::toAssignmentResponse);
    }
}