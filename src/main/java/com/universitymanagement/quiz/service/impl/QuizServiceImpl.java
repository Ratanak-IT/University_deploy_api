package com.universitymanagement.quiz.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.quiz.dto.request.AddQuizQuestionRequest;
import com.universitymanagement.quiz.dto.request.AssignQuizToClassroomRequest;
import com.universitymanagement.quiz.dto.request.CreateQuizRequest;
import com.universitymanagement.quiz.dto.response.QuizAttemptSummaryResponse;
import com.universitymanagement.quiz.dto.response.QuizClassroomResponse;
import com.universitymanagement.quiz.dto.response.QuizManageResponse;
import com.universitymanagement.quiz.entity.Quiz;
import com.universitymanagement.quiz.entity.QuizAssignment;
import com.universitymanagement.quiz.entity.AttemptStatus;
import com.universitymanagement.quiz.entity.QuizAttempt;
import com.universitymanagement.quiz.entity.QuizQuestion;
import com.universitymanagement.quiz.exception.QuizAccessDeniedException;
import com.universitymanagement.quiz.exception.QuizClassroomNotFoundException;
import com.universitymanagement.quiz.exception.QuizNotFoundException;
import com.universitymanagement.quiz.repository.QuizAssignmentRepository;
import com.universitymanagement.quiz.repository.QuizAttemptRepository;
import com.universitymanagement.quiz.repository.QuizRepository;
import com.universitymanagement.quiz.service.QuizService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.exception.TeacherNotFoundException;
import com.universitymanagement.teacher.repository.TeacherRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.universitymanagement.quiz.entity.QuestionType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizAssignmentRepository assignmentRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final com.universitymanagement.classroom.repository.ClassroomStudentRepository classroomStudentRepository;
    private final com.universitymanagement.notification.service.NotificationService notificationService;

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

            boolean isNewRelease = assignmentRepository
                    .findByQuiz_QuizIdAndClassroom_ClassroomId(quizId, release.classroomId())
                    .isEmpty();

            QuizAssignment assignment = assignmentRepository
                    .findByQuiz_QuizIdAndClassroom_ClassroomId(quizId, release.classroomId())
                    .orElseGet(QuizAssignment::new);

            assignment.setQuiz(quiz);
            assignment.setClassroom(classroom);
            assignment.setAvailableFrom(release.availableFrom());
            assignment.setAvailableTo(release.availableTo());

            assignmentRepository.save(assignment);

            // Only the first release to a section, not every subsequent edit of
            // its window — otherwise narrowing the availableFrom/To dates would
            // re-notify a class that already knows the quiz exists.
            if (isNewRelease) {
                notifyClassroomOfNewQuiz(classroom, quiz);
            }
        }

        return toManageResponse(quiz);
    }

    /**
     * Tells every enrolled student a quiz has been released to their class.
     *
     * <p>Best-effort: a notification failure (or a student with no linked user
     * account) must not roll back the release itself, which is the part that
     * actually matters here.
     */
    private void notifyClassroomOfNewQuiz(Classroom classroom, Quiz quiz) {
        String teacherName = quiz.getCreatedByTeacher() != null
                && quiz.getCreatedByTeacher().getUser() != null
                ? quiz.getCreatedByTeacher().getUser().getFullName()
                : "Your teacher";

        for (com.universitymanagement.classroom.entity.ClassroomStudent link
                : classroomStudentRepository.findRosterWithUser(classroom.getClassroomId())) {
            com.universitymanagement.student.entity.Student student = link.getStudent();
            if (student == null || student.getUser() == null) continue;

            try {
                notificationService.createNotification(
                        student.getUser().getId(),
                        "New quiz: " + quiz.getTitle(),
                        quiz.getTitle() + " has been released in " + classroom.getClassName() + ".",
                        "ANNOUNCEMENT",
                        classroom.getClassName(),
                        teacherName,
                        // The Quizzes tab of the right classroom, not the
                        // classroom's front door — matches the courses page's
                        // own tab labels exactly ("Quizzes", case-sensitive).
                        "/dashboard/student/courses?classroomId=" + classroom.getClassroomId() + "&tab=Quizzes",
                        "CLASSROOM",
                        classroom.getClassroomId()
                );
            } catch (Exception e) {
                log.warn("New-quiz notification failed for student {}: {}",
                        student.getStudentId(), e.getMessage());
            }
        }
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
        question.setScore(request.score());
        question.setQuestionOrder(nextOrder);
        applyAnswer(question, request.type(), request.options(),
                request.correctOptionIndex(), request.correctAnswer());
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

    @Override
    public List<QuizAttemptSummaryResponse> getAttemptsForQuiz(UUID quizId) {
        findOwnedQuiz(quizId);

        // The full roster of every classroom the quiz was released to, not
        // just the students who happened to attempt it — a teacher filtering
        // for "not started" needs those rows to exist at all.
        List<UUID> classroomIds = assignmentRepository.findByQuizWithClassroom(quizId).stream()
                .map(a -> a.getClassroom().getClassroomId())
                .distinct()
                .toList();

        if (classroomIds.isEmpty()) {
            return List.of();
        }

        // One row per (classroom, student) enrolment rather than deduped by
        // student — a quiz released to two sections should show which of
        // them each row belongs to, not silently collapse a student into a
        // single unlabeled row.
        List<ClassroomStudent> roster = classroomStudentRepository.findRosterWithUserByClassroomIds(classroomIds);

        Map<UUID, QuizAttempt> attemptByStudent = attemptRepository.findByQuiz_QuizIdWithStudent(quizId)
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getStudent().getStudentId(),
                        a -> a,
                        // A student can have more than one attempt (a resumed
                        // one after an expiry): keep the settled one over an
                        // abandoned in-progress one, then the most recent.
                        (a, b) -> {
                            boolean aSettled = a.getStatus() != AttemptStatus.IN_PROGRESS;
                            boolean bSettled = b.getStatus() != AttemptStatus.IN_PROGRESS;
                            if (aSettled != bSettled) return aSettled ? a : b;
                            return a.getStartedAt().isAfter(b.getStartedAt()) ? a : b;
                        }));

        return roster.stream()
                .map(cs -> toAttemptSummary(cs, attemptByStudent.get(cs.getStudent().getStudentId())))
                .toList();
    }

    private QuizAttemptSummaryResponse toAttemptSummary(ClassroomStudent cs, QuizAttempt a) {
        Student student = cs.getStudent();
        var user = student.getUser();
        String studentName = user != null ? user.getFullName() : null;
        UUID classroomId = cs.getClassroom().getClassroomId();
        String className = cs.getClassroom().getClassName();

        if (a == null) {
            return new QuizAttemptSummaryResponse(
                    null,
                    student.getStudentId(),
                    student.getStudentCode(),
                    studentName,
                    classroomId,
                    className,
                    "NOT_STARTED",
                    null,
                    null,
                    null,
                    null,
                    // Nobody who has not started can have left the screen.
                    0,
                    null
            );
        }

        return new QuizAttemptSummaryResponse(
                a.getAttemptId(),
                student.getStudentId(),
                student.getStudentCode(),
                studentName,
                classroomId,
                className,
                a.getStatus().name(),
                a.getStartedAt(),
                a.getSubmittedAt(),
                a.getEarnedScore(),
                a.getTotalScore(),
                a.getFocusLossCount(),
                a.getLastFocusLossAt()
        );
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
        question.setScore(item.score());
        question.setQuestionOrder(item.questionOrder() != null ? item.questionOrder() : fallbackOrder);
        applyAnswer(question, item.type(), item.options(),
                item.correctOptionIndex(), item.correctAnswer());
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

    /**
     * Writes the options and the correct answer onto a question, refusing
     * anything that could not be answered correctly.
     *
     * <p>This validation lives here rather than in bean annotations because it
     * is a relationship *between* fields: an index is only meaningful against
     * the option list it points into. The client already guards this, but a
     * client-side check is not a control — the API is reachable directly, and
     * without this a question could be stored whose correct answer matches no
     * option at all, which no student could ever get right.
     */
    private void applyAnswer(QuizQuestion question,
                             QuestionType requestedType,
                             List<String> options,
                             Integer correctOptionIndex,
                             String correctAnswer) {

        QuestionType type = requestedType != null ? requestedType : QuestionType.MULTIPLE_CHOICE;
        question.setType(type);

        if (type == QuestionType.SHORT_ANSWER) {
            if (correctAnswer == null || correctAnswer.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A short-answer question needs a correct answer.");
            }
            question.setOptionsJson(writeOptions(List.of()));
            question.setCorrectOptionIndex(null);
            question.setCorrectAnswer(correctAnswer.trim());
            return;
        }

        List<String> cleaned = options == null
                ? List.of()
                : options.stream()
                        .filter(o -> o != null && !o.isBlank())
                        .map(String::trim)
                        .toList();

        if (cleaned.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A choice question needs at least two options.");
        }
        if (type == QuestionType.TRUE_FALSE && cleaned.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A true/false question needs exactly two options.");
        }

        Integer index = correctOptionIndex;

        // No index sent: fall back to locating the answer's text, so a client
        // written against the old contract keeps working.
        if (index == null && correctAnswer != null && !correctAnswer.isBlank()) {
            String wanted = correctAnswer.trim();
            for (int i = 0; i < cleaned.size(); i++) {
                if (cleaned.get(i).equalsIgnoreCase(wanted)) {
                    index = i;
                    break;
                }
            }
            if (index == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The correct answer \"" + wanted + "\" is not one of the options.");
            }
        }

        if (index == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A choice question needs correctOptionIndex.");
        }
        if (index < 0 || index >= cleaned.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "correctOptionIndex " + index + " is outside the "
                            + cleaned.size() + " options supplied.");
        }

        question.setOptionsJson(writeOptions(cleaned));
        question.setCorrectOptionIndex(index);
        // Mirror kept in step with the index so exports stay readable.
        question.setCorrectAnswer(cleaned.get(index));
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
                        q.getCorrectOptionIndex(),
                        q.getCorrectAnswer(),
                        q.getType(),
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
