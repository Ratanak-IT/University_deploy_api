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
import com.universitymanagement.quiz.dto.response.QuizManageResponse;
import com.universitymanagement.quiz.entity.Quiz;
import com.universitymanagement.quiz.entity.QuizQuestion;
import com.universitymanagement.quiz.exception.QuizAccessDeniedException;
import com.universitymanagement.quiz.exception.QuizClassroomNotFoundException;
import com.universitymanagement.quiz.exception.QuizNotFoundException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuizManageResponse createQuiz(CreateQuizRequest request) {
        Teacher teacher = currentTeacher();

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
        Classroom classroom = classroomRepository.findById(request.classroomId())
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new QuizClassroomNotFoundException(request.classroomId()));

        quiz.setClassroom(classroom);
        return toManageResponse(quizRepository.save(quiz));
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
    public List<QuizManageResponse> getMyQuizzes() {
        Teacher teacher = currentTeacher();
        return quizRepository
                .findByCreatedByTeacher_TeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(teacher.getTeacherId())
                .stream()
                .map(this::toManageResponse)
                .toList();
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

        return new QuizManageResponse(
                quiz.getQuizId(),
                quiz.getClassroom() != null ? quiz.getClassroom().getClassroomId() : null,
                quiz.getClassroom() != null ? quiz.getClassroom().getClassName() : null,
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getStartAt(),
                quiz.getEndAt(),
                quiz.getDurationMinutes(),
                quiz.getMaxAttempts(),
                quiz.getClassroom() != null ? "PUBLISHED" : "DRAFT",
                questions
        );
    }
}
