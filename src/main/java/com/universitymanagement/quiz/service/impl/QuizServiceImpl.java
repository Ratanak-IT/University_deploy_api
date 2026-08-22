package com.universitymanagement.quiz.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.quiz.dto.request.AddQuizQuestionRequest;
import com.universitymanagement.quiz.dto.request.AssignQuizToClassroomRequest;
import com.universitymanagement.quiz.dto.request.CreateQuizRequest;
import com.universitymanagement.quiz.dto.response.QuizClassroomResponse;
import com.universitymanagement.quiz.dto.response.QuizManageResponse;
import com.universitymanagement.quiz.entity.Quiz;
import com.universitymanagement.quiz.entity.QuizAssignment;
import com.universitymanagement.quiz.entity.QuizQuestion;
import com.universitymanagement.quiz.exception.QuizAccessDeniedException;
import com.universitymanagement.quiz.exception.QuizClassroomNotFoundException;
import com.universitymanagement.quiz.exception.QuizNotFoundException;
import com.universitymanagement.quiz.repository.QuizAssignmentRepository;
import com.universitymanagement.quiz.repository.QuizAttemptRepository;
import com.universitymanagement.quiz.repository.QuizRepository;
import com.universitymanagement.quiz.service.QuizService;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.exception.TeacherNotFoundException;
import com.universitymanagement.teacher.repository.TeacherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizAssignmentRepository assignmentRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuizManageResponse createQuiz(CreateQuizRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Teacher teacher = null;
        if (!hasRole(authentication, "ADMIN")) {
            teacher = currentTeacher();
        } else {
            User user = getCurrentUser(authentication);
            teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
        }

        Quiz quiz = new Quiz();
        quiz.setClassroom(null);
        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setStartAt(request.startAt());
        quiz.setEndAt(request.endAt());
        quiz.setDurationMinutes(request.durationMinutes());
        quiz.setMaxAttempts(request.maxAttempts() != null ? request.maxAttempts() : 1);
        quiz.setCreatedByTeacher(teacher);
        quiz.setIsDeleted(false);

        if (request.questions() != null) {
            int order = 0;
            for (CreateQuizRequest.QuestionItem item : request.questions()) {
                quiz.getQuestions().add(toQuestionEntity(quiz, item, order++));
            }
        }

        return toManageResponse(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public QuizManageResponse assignToClassroom(UUID quizId, AssignQuizToClassroomRequest request) {
        Quiz quiz = findOwnedQuiz(quizId);

        List<AssignQuizToClassroomRequest.ClassroomRelease> releases = request.releases();
        if (releases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pick at least one classroom to release this quiz to.");
        }

        List<QuizAssignment> existing = assignmentRepository.findByQuizWithClassroom(quizId);
        Set<UUID> keep = releases.stream()
                .map(AssignQuizToClassroomRequest.ClassroomRelease::classroomId)
                .collect(Collectors.toSet());

        // Pulling a quiz from a section it was never released to is a no-op, but
        // pulling it from one where students have already sat it would strand
        // their attempts — so that is refused rather than silently discarded.
        for (QuizAssignment assignment : existing) {
            UUID classroomId = assignment.getClassroom().getClassroomId();
            if (keep.contains(classroomId)) {
                continue;
            }
            long attempts = attemptRepository.countByQuiz_QuizIdAndClassroom(quizId, classroomId);
            if (attempts > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "\"" + assignment.getClassroom().getClassName() + "\" already has "
                                + attempts + " attempt(s) on this quiz and cannot be removed.");
            }
            assignmentRepository.delete(assignment);
        }

        for (AssignQuizToClassroomRequest.ClassroomRelease release : releases) {
            Classroom classroom = classroomRepository.findById(release.classroomId())
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .orElseThrow(() -> new QuizClassroomNotFoundException(release.classroomId()));

            QuizAssignment assignment = assignmentRepository
                    .findByQuiz_QuizIdAndClassroom_ClassroomId(quizId, release.classroomId())
                    .orElseGet(QuizAssignment::new);

            assignment.setQuiz(quiz);
            assignment.setClassroom(classroom);
            assignment.setAvailableFrom(release.availableFrom());
            assignment.setAvailableTo(release.availableTo());

            assignmentRepository.save(assignment);
        }

        return toManageResponse(quiz);
    }

    @Override
    @Transactional
    public QuizManageResponse addQuestion(UUID quizId, AddQuizQuestionRequest request) {
        Quiz quiz = findOwnedQuiz(quizId);
        int nextOrder = request.questionOrder() != null
                ? request.questionOrder()
                : quiz.getQuestions().size();

        QuizQuestion question = new QuizQuestion();
        question.setQuiz(quiz);
        question.setQuestionText(request.questionText());
        question.setOptionsJson(writeOptions(request.options()));
        question.setCorrectAnswer(request.correctAnswer());
        question.setScore(request.score());
        question.setQuestionOrder(nextOrder);
        quiz.getQuestions().add(question);

        return toManageResponse(quizRepository.save(quiz));
    }

    @Override
    public QuizManageResponse getQuiz(UUID quizId) {
        return toManageResponse(findOwnedQuiz(quizId));
    }

    @Override
    @Transactional
    public QuizManageResponse updateQuiz(UUID quizId, CreateQuizRequest request) {
        Quiz quiz = findOwnedQuiz(quizId);
        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setStartAt(request.startAt());
        quiz.setEndAt(request.endAt());
        if (request.durationMinutes() != null) {
            quiz.setDurationMinutes(request.durationMinutes());
        }
        if (request.maxAttempts() != null) {
            quiz.setMaxAttempts(request.maxAttempts());
        }

        if (request.questions() != null) {
            quiz.getQuestions().clear();
            int order = 0;
            for (CreateQuizRequest.QuestionItem item : request.questions()) {
                quiz.getQuestions().add(toQuestionEntity(quiz, item, order++));
            }
        }

        return toManageResponse(quizRepository.save(quiz));
    }

    @Override
    public List<QuizManageResponse> getMyQuizzes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(authentication, "ADMIN")) {
            return quizRepository.findAll().stream()
                    .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .map(this::toManageResponse)
                    .toList();
        }
        Teacher teacher = currentTeacher();
        return quizRepository
                .findByCreatedByTeacher_TeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(teacher.getTeacherId())
                .stream()
                .map(this::toManageResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteQuiz(UUID quizId) {
        Quiz quiz = findOwnedQuiz(quizId);
        quiz.setIsDeleted(true);
        quizRepository.save(quiz);
        assignmentRepository.deleteByQuiz_QuizId(quizId);
    }


    private Quiz findOwnedQuiz(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(authentication, "ADMIN")) {
            return quiz;
        }
        Teacher teacher = currentTeacher();
        boolean owns = quiz.getCreatedByTeacher() != null
                && quiz.getCreatedByTeacher().getTeacherId().equals(teacher.getTeacherId());
        if (!owns) {
            throw new QuizAccessDeniedException(quizId);
        }
        return quiz;
    }

    private Teacher currentTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getCurrentUser(authentication);
        if (hasRole(authentication, "ADMIN")) {
            return teacherRepository.findByUserId(user.getId()).orElse(null);
        }
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new TeacherNotFoundException(
                        "Teacher profile not found for current user"));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserNotFoundException();
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(UserNotFoundException::new);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private QuizQuestion toQuestionEntity(Quiz quiz, CreateQuizRequest.QuestionItem item, int fallbackOrder) {
        QuizQuestion question = new QuizQuestion();
        question.setQuiz(quiz);
        question.setQuestionText(item.questionText());
        question.setOptionsJson(writeOptions(item.options()));
        question.setCorrectAnswer(item.correctAnswer());
        question.setScore(item.score());
        question.setQuestionOrder(item.questionOrder() != null ? item.questionOrder() : fallbackOrder);
        return question;
    }

    private String writeOptions(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize options");
        }
    }

    private List<String> readOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private QuizManageResponse toManageResponse(Quiz quiz) {
        List<QuizManageResponse.QuizQuestionManageResponse> questions = quiz.getQuestions().stream()
                .sorted((a, b) -> {
                    Integer oa = a.getQuestionOrder() != null ? a.getQuestionOrder() : 0;
                    Integer ob = b.getQuestionOrder() != null ? b.getQuestionOrder() : 0;
                    return oa.compareTo(ob);
                })
                .map(q -> new QuizManageResponse.QuizQuestionManageResponse(
                        q.getQuestionId(),
                        q.getQuestionText(),
                        readOptions(q.getOptionsJson()),
                        q.getCorrectAnswer(),
                        q.getScore(),
                        q.getQuestionOrder()))
                .toList();

        List<QuizClassroomResponse> classrooms = assignmentRepository
                .findByQuizWithClassroom(quiz.getQuizId())
                .stream()
                .map(a -> new QuizClassroomResponse(
                        a.getAssignmentId(),
                        a.getClassroom().getClassroomId(),
                        a.getClassroom().getClassName(),
                        a.getClassroom().getClassCode(),
                        a.getClassroom().getSubject() != null
                                ? a.getClassroom().getSubject().getSubjectName() : null,
                        a.getAvailableFrom(),
                        a.getAvailableTo()))
                .toList();

        QuizClassroomResponse first = classrooms.isEmpty() ? null : classrooms.getFirst();

        return new QuizManageResponse(
                quiz.getQuizId(),
                first != null ? first.classroomId() : null,
                first != null ? first.className() : null,
                classrooms,
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getStartAt(),
                quiz.getEndAt(),
                quiz.getDurationMinutes(),
                quiz.getMaxAttempts(),
                classrooms.isEmpty() ? "DRAFT" : "PUBLISHED",
                questions
        );
    }
}
