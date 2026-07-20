package com.universitymanagement.score.service.impl;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.score.dto.request.SetExamScoresRequest;
import com.universitymanagement.score.dto.response.ExamScoreResponse;
import com.universitymanagement.score.entity.ExamScore;
import com.universitymanagement.score.exception.InvalidExamScoreException;
import com.universitymanagement.score.exception.StudentNotInClassroomException;
import com.universitymanagement.score.repository.ExamScoreRepository;
import com.universitymanagement.score.service.ExamScoreService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.repository.TeacherRepository;
import com.universitymanagement.assignment.exception.AssignmentClassroomNotFoundException;
import com.universitymanagement.assignment.exception.ClassroomHasNoTeacherException;
import com.universitymanagement.assignment.exception.NotClassroomTeacherException;
import com.universitymanagement.assignment.exception.TeacherProfileNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamScoreServiceImpl implements ExamScoreService {

    private final ExamScoreRepository examScoreRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<ExamScoreResponse> setScores(UUID classroomId, SetExamScoresRequest request) {
        Classroom classroom = findClassroom(classroomId);
        Teacher teacher = requireTeacherOwnsClassroom(classroom);

        List<ExamScore> saved = request.scores().stream()
                .map(item -> {
                    if (item.score() > request.maxScore()) {
                        throw new InvalidExamScoreException(
                                "Score " + item.score() + " exceeds maxScore " + request.maxScore()
                                        + " for student " + item.studentId());
                    }

                    Student student = studentRepository.findById(item.studentId())
                            .orElseThrow(() -> new StudentNotInClassroomException(
                                    item.studentId(), classroomId));

                    boolean enrolled = classroomStudentRepository
                            .existsByClassroom_ClassroomIdAndStudent_StudentId(
                                    classroomId, student.getStudentId());
                    if (!enrolled) {
                        throw new StudentNotInClassroomException(student.getStudentId(), classroomId);
                    }

                    ExamScore examScore = examScoreRepository
                            .findByStudent_StudentIdAndClassroom_ClassroomIdAndExamType(
                                    student.getStudentId(), classroomId, request.examType())
                            .orElseGet(ExamScore::new);

                    examScore.setStudent(student);
                    examScore.setClassroom(classroom);
                    examScore.setExamType(request.examType());
                    examScore.setScore(item.score());
                    examScore.setMaxScore(request.maxScore());
                    examScore.setEnteredByTeacher(teacher);

                    return examScoreRepository.save(examScore);
                })
                .toList();

        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    public List<ExamScoreResponse> getScoresByClassroom(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        requireTeacherOwnsClassroomOrAdmin(classroom);

        return examScoreRepository
                .findByClassroom_ClassroomId(classroomId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ---- helpers ----

    private ExamScoreResponse toResponse(ExamScore e) {
        return new ExamScoreResponse(
                e.getExamScoreId(),
                e.getStudent().getStudentId(),
                e.getStudent().getStudentCode(),
                e.getStudent().getUser() != null ? e.getStudent().getUser().getFullName() : null,
                e.getClassroom().getClassroomId(),
                e.getExamType(),
                e.getScore(),
                e.getMaxScore()
        );
    }

    private Classroom findClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AssignmentClassroomNotFoundException(classroomId));
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