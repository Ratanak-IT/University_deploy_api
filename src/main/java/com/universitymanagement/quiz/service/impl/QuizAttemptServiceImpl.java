package com.universitymanagement.quiz.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.quiz.dto.request.SubmitQuizAttemptRequest;
import com.universitymanagement.quiz.dto.response.QuizAttemptResponse;
import com.universitymanagement.quiz.dto.response.QuizQuestionResponse;
import com.universitymanagement.quiz.dto.response.QuizResponse;
import com.universitymanagement.quiz.entity.*;
import com.universitymanagement.quiz.exception.QuizAttemptAlreadyFinalizedException;
import com.universitymanagement.quiz.exception.QuizAttemptNotFoundException;
import com.universitymanagement.quiz.exception.QuizMaxAttemptsReachedException;
import com.universitymanagement.quiz.exception.QuizNotFoundException;
import com.universitymanagement.quiz.exception.QuizWindowClosedException;
import com.universitymanagement.quiz.exception.StudentNotEnrolledInQuizException;
import com.universitymanagement.quiz.repository.QuizAttemptRepository;
import com.universitymanagement.quiz.repository.QuizQuestionRepository;
import com.universitymanagement.quiz.entity.QuizAssignment;
import com.universitymanagement.quiz.repository.QuizAssignmentRepository;
import com.universitymanagement.quiz.repository.QuizRepository;
import com.universitymanagement.quiz.service.QuizAttemptService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.security.StudentAccessGuard;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAssignmentRepository assignmentRepository;
    private final StudentAccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    @Override
    public List<QuizResponse> getQuizzesForStudent(UUID studentId) {
        accessGuard.requireSelfOrStaff(studentId);
        return assignmentRepository.findAllForStudent(studentId)
                .stream()
                .map(assignment -> toQuizResponse(assignment, studentId))
                .toList();
    }

    @Override
    @Transactional
    public QuizAttemptResponse startAttempt(UUID studentId, UUID quizId) {
        Student student = accessGuard.requireSelf(studentId);
        Quiz quiz = findQuiz(quizId);
        QuizAssignment release = requireRelease(quiz, studentId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime opensAt = release.effectiveFrom();
        LocalDateTime closesAt = release.effectiveTo();

        if (opensAt != null && now.isBefore(opensAt)) {
            throw new QuizWindowClosedException(quizId, "quiz has not started yet");
        }
        if (closesAt != null && now.isAfter(closesAt)) {
            throw new QuizWindowClosedException(quizId, "quiz window is already closed");
        }

        long used = quizAttemptRepository.countByQuiz_QuizIdAndStudent_StudentId(quizId, studentId);
        if (quiz.getMaxAttempts() != null && used >= quiz.getMaxAttempts()) {
            throw new QuizMaxAttemptsReachedException(quizId, quiz.getMaxAttempts());
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setStartedAt(now);
        if (quiz.getDurationMinutes() != null) {
            attempt.setExpiresAt(now.plusMinutes(quiz.getDurationMinutes()));
        }
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setTotalScore(sumTotalScore(quizId));

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        return toAttemptResponse(saved, true, false);
    }

    @Override
    @Transactional
    public QuizAttemptResponse submitAttempt(UUID studentId, UUID quizId, UUID attemptId,
                                             SubmitQuizAttemptRequest request) {
        accessGuard.requireSelf(studentId);
        QuizAttempt attempt = findAttempt(attemptId, quizId, studentId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new QuizAttemptAlreadyFinalizedException(attemptId, attempt.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        boolean expired = attempt.getExpiresAt() != null && now.isAfter(attempt.getExpiresAt());

        List<QuizQuestion> questions =
                quizQuestionRepository.findByQuiz_QuizIdOrderByQuestionOrderAsc(quizId);
        Map<UUID, SubmitQuizAttemptRequest.AnswerItem> submitted = new HashMap<>();
        request.answers().forEach(a -> submitted.put(a.questionId(), a));

        double earned = 0.0;
        attempt.getAnswers().clear();
        for (QuizQuestion question : questions) {
            SubmitQuizAttemptRequest.AnswerItem item = submitted.get(question.getQuestionId());
            String answer = item != null ? item.answer() : null;
            Integer picked = item != null ? item.selectedOptionIndex() : null;

            boolean correct = !expired && isCorrect(question, picked, answer);

            QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
            attemptAnswer.setAttempt(attempt);
            attemptAnswer.setQuestion(question);
            attemptAnswer.setAnswer(answerText(question, picked, answer));
            attemptAnswer.setIsCorrect(correct);
            attemptAnswer.setEarnedScore(correct ? question.getScore() : 0.0);
            attempt.getAnswers().add(attemptAnswer);

            earned += correct ? question.getScore() : 0.0;
        }

        attempt.setSubmittedAt(now);
        attempt.setEarnedScore(earned);
        attempt.setStatus(expired ? AttemptStatus.EXPIRED : AttemptStatus.SUBMITTED);

        return toAttemptResponse(quizAttemptRepository.save(attempt), false, true);
    }

    @Override
    public QuizAttemptResponse getAttemptResult(UUID studentId, UUID quizId, UUID attemptId) {
        accessGuard.requireSelfOrStaff(studentId);
        QuizAttempt attempt = findAttempt(attemptId, quizId, studentId);
        boolean showAnswers = attempt.getStatus() != AttemptStatus.IN_PROGRESS;
        return toAttemptResponse(attempt, !showAnswers, showAnswers);
    }

    /**
     * Whether an answer earns its marks.
     *
     * <p>Choice questions are decided on the index, so rewording an option can
     * no longer silently invalidate every answer. The text path is kept only
     * for clients that still submit the option's label, and for SHORT_ANSWER
     * where text is all there is.
     */
    private boolean isCorrect(QuizQuestion question, Integer picked, String answer) {
        if (question.getType() == QuestionType.SHORT_ANSWER) {
            return answer != null
                    && question.getCorrectAnswer() != null
                    && answer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
        }

        Integer correctIndex = question.getCorrectOptionIndex();

        if (picked != null && correctIndex != null) {
            return picked.intValue() == correctIndex.intValue();
        }

        // Older client sent the option text: resolve it to an index rather than
        // comparing against the stored label, which may since have been edited.
        if (answer != null && correctIndex != null) {
            List<String> options = readOptions(question.getOptionsJson());
            String wanted = answer.trim();
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).trim().equalsIgnoreCase(wanted)) {
                    return i == correctIndex.intValue();
                }
            }
            return false;
        }

        // Nothing indexed yet (a row the migration could not resolve): fall
        // back to the historical text comparison rather than failing everyone.
        return answer != null
                && question.getCorrectAnswer() != null
                && answer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
    }

    /** What to record as the student's answer, for the result screen. */
    private String answerText(QuizQuestion question, Integer picked, String answer) {
        if (picked != null && question.getType() != QuestionType.SHORT_ANSWER) {
            List<String> options = readOptions(question.getOptionsJson());
            if (picked >= 0 && picked < options.size()) {
                return options.get(picked);
            }
        }
        return answer;
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

    private Quiz findQuiz(UUID quizId) {
        return quizRepository.findById(quizId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new QuizNotFoundException(quizId));
    }

    private QuizAttempt findAttempt(UUID attemptId, UUID quizId, UUID studentId) {
        return quizAttemptRepository
                .findByAttemptIdAndQuiz_QuizIdAndStudent_StudentId(attemptId, quizId, studentId)
                .orElseThrow(() -> new QuizAttemptNotFoundException(attemptId));
    }

    /**
     * The release this student sits under. A student enrolled in two sections
     * that both received the quiz gets the most generous window, since either
     * would let them in on its own.
     */
    private QuizAssignment requireRelease(Quiz quiz, UUID studentId) {
        return assignmentRepository.findForStudentAndQuiz(quiz.getQuizId(), studentId)
                .stream()
                .max(java.util.Comparator.comparing(
                        QuizAssignment::effectiveTo,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .orElseThrow(() -> new StudentNotEnrolledInQuizException(studentId, quiz.getQuizId()));
    }

    private void requireEnrolled(Quiz quiz, UUID studentId) {
        requireRelease(quiz, studentId);
    }

    private Double sumTotalScore(UUID quizId) {
        return quizQuestionRepository.findByQuiz_QuizIdOrderByQuestionOrderAsc(quizId)
                .stream()
                .mapToDouble(QuizQuestion::getScore)
                .sum();
    }

    private QuizResponse toQuizResponse(QuizAssignment release, UUID studentId) {
        Quiz quiz = release.getQuiz();
        long used = quizAttemptRepository
                .countByQuiz_QuizIdAndStudent_StudentId(quiz.getQuizId(), studentId);
        Double best = quizAttemptRepository
                .findByStudent_StudentIdOrderByStartedAtDesc(studentId)
                .stream()
                .filter(a -> a.getQuiz().getQuizId().equals(quiz.getQuizId()))
                .map(QuizAttempt::getEarnedScore)
                .filter(java.util.Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        return new QuizResponse(
                quiz.getQuizId(),
                release.getClassroom().getClassroomId(),
                release.getClassroom().getClassName(),
                release.getClassroom().getSubject() != null
                        ? release.getClassroom().getSubject().getSubjectName() : null,
                quiz.getTitle(),
                quiz.getDescription(),
                release.effectiveFrom(),
                release.effectiveTo(),
                quiz.getDurationMinutes(),
                quiz.getMaxAttempts(),
                used,
                best
        );
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt,
                                                  boolean includeQuestions,
                                                  boolean includeAnswers) {
        List<QuizQuestionResponse> questions = includeQuestions
                ? quizQuestionRepository
                        .findByQuiz_QuizIdOrderByQuestionOrderAsc(attempt.getQuiz().getQuizId())
                        .stream()
                        .map(this::toQuestionResponse)
                        .toList()
                : List.of();

        List<QuizAttemptResponse.AnswerResult> answers = includeAnswers
                ? attempt.getAnswers().stream()
                        .map(a -> new QuizAttemptResponse.AnswerResult(
                                a.getQuestion().getQuestionId(),
                                a.getAnswer(),
                                a.getIsCorrect(),
                                a.getEarnedScore()))
                        .toList()
                : List.of();

        return new QuizAttemptResponse(
                attempt.getAttemptId(),
                attempt.getQuiz().getQuizId(),
                attempt.getQuiz().getTitle(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                attempt.getSubmittedAt(),
                attempt.getEarnedScore(),
                attempt.getTotalScore(),
                questions,
                answers
        );
    }

    private QuizQuestionResponse toQuestionResponse(QuizQuestion question) {
        List<String> options = List.of();
        if (question.getOptionsJson() != null && !question.getOptionsJson().isBlank()) {
            try {
                options = objectMapper.readValue(
                        question.getOptionsJson(), new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
            }
        }
        return new QuizQuestionResponse(
                question.getQuestionId(),
                question.getQuestionText(),
                options,
                question.getType(),
                question.getScore(),
                question.getQuestionOrder()
        );
    }
}
