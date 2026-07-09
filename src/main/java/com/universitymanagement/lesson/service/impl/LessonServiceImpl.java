package com.universitymanagement.lesson.service.impl;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.lesson.dto.request.LessonRequest;
import com.universitymanagement.lesson.dto.response.LessonResponse;
import com.universitymanagement.lesson.entity.Lesson;
import com.universitymanagement.lesson.repository.LessonRepository;
import com.universitymanagement.lesson.service.LessonService;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public LessonResponse createLesson(UUID classroomId, LessonRequest request, MultipartFile file) {
        Classroom classroom = findClassroom(classroomId);
        requireTeacherOwnsClassroom(classroom);

        Lesson lesson = new Lesson();
        lesson.setClassroom(classroom);
        applyRequest(lesson, request, file);

        return toResponse(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request, MultipartFile file) {
        Lesson lesson = findLesson(lessonId);
        requireTeacherOwnsClassroom(lesson.getClassroom());

        applyRequest(lesson, request, file);

        return toResponse(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public void deleteLesson(UUID lessonId) {
        Lesson lesson = findLesson(lessonId);
        requireTeacherOwnsClassroom(lesson.getClassroom());
        lesson.setIsDeleted(true);
        lessonRepository.save(lesson);
    }

    @Override
    public List<LessonResponse> getLessonsByClassroom(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        requireMemberOrAdmin(classroom);

        return lessonRepository
                .findByClassroom_ClassroomIdAndIsDeletedFalseOrderByCreatedAtDesc(classroomId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private void applyRequest(Lesson lesson, LessonRequest request, MultipartFile file) {
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setVideoLink(request.videoLink());
        lesson.setAllowDownload(Boolean.TRUE.equals(request.allowDownload()));

        if (file != null && !file.isEmpty()) {
            String objectName = minioService.uploadFile(file);
            lesson.setFileObjectName(objectName);
            lesson.setFileOriginalName(file.getOriginalFilename());
        }
    }

    private LessonResponse toResponse(Lesson lesson) {
        String fileUrl = lesson.getFileObjectName() != null
                ? minioService.getPreviewUrl(lesson.getFileObjectName())
                : null;

        return new LessonResponse(
                lesson.getLessonId(),
                lesson.getClassroom().getClassroomId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getFileOriginalName(),
                fileUrl,
                lesson.getVideoLink(),
                lesson.getAllowDownload(),
                lesson.getCreatedAt(),
                lesson.getCreatedBy()
        );
    }

    private Lesson findLesson(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    private Classroom findClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found"));
    }

    /** Only the assigned teacher of this classroom may create/edit lessons. */
    private void requireTeacherOwnsClassroom(Classroom classroom) {
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
    }

    /** ADMIN, the classroom's teacher, or an enrolled student may view lessons. */
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
