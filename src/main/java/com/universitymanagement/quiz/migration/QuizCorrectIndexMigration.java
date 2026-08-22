package com.universitymanagement.quiz.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitymanagement.quiz.entity.QuizQuestion;
import com.universitymanagement.quiz.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fills in {@code correctOptionIndex} for questions written before the column
 * existed, by locating the stored answer text among that question's options.
 *
 * <p>Runs once: questions that already carry an index are skipped, so a restart
 * costs one query and nothing else.
 *
 * <p>The text column is deliberately left populated. It is what this migration
 * reads, it keeps exports readable, and grading still falls back to it for any
 * row this cannot resolve — so a question that fails to migrate keeps behaving
 * exactly as it did before rather than breaking.
 */
@Component
@RequiredArgsConstructor
@Order(130)
@Slf4j
public class QuizCorrectIndexMigration implements ApplicationRunner {

    private final QuizQuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // A migration must never be able to take the application down. If the
        // schema is not in the shape this expects — a prior DDL step failed,
        // a manual change is mid-flight, whatever — that is a reason to skip
        // this run and let a human look at the log, not a reason the whole
        // app refuses to start. (This guard exists because exactly that
        // happened: `question_type` failed to migrate on a populated table
        // before its DEFAULT was fixed, and the unguarded query below turned
        // that missing column into a startup crash instead of a warning.)
        try {
            migrate();
        } catch (Exception e) {
            log.warn("Quiz correct-answer migration skipped: {}", e.getMessage());
        }
    }

    private void migrate() {
        List<QuizQuestion> questions = questionRepository.findAll();

        int migrated = 0;
        int unresolved = 0;

        for (QuizQuestion question : questions) {
            if (question.getCorrectOptionIndex() != null) {
                continue;
            }

            String answer = question.getCorrectAnswer();
            if (answer == null || answer.isBlank()) {
                continue;
            }

            List<String> options = readOptions(question.getOptionsJson());
            if (options.isEmpty()) {
                // No options at all — nothing to index against. A short-answer
                // question looks exactly like this, and is correct as it is.
                continue;
            }

            int index = indexOf(options, answer);
            if (index < 0) {
                // The stored answer matches none of the options. This is the
                // very failure the index exists to prevent, and it is worth
                // naming loudly: nobody can currently answer this question.
                unresolved++;
                log.warn(
                        "Quiz question {} has correctAnswer \"{}\" which matches none of its {} options. "
                                + "It cannot be answered correctly until a teacher fixes it.",
                        question.getQuestionId(), answer, options.size());
                continue;
            }

            question.setCorrectOptionIndex(index);
            questionRepository.save(question);
            migrated++;
        }

        if (migrated > 0 || unresolved > 0) {
            log.info("Quiz correct-answer migration: {} question(s) indexed, {} unresolved.",
                    migrated, unresolved);
        }
    }

    private int indexOf(List<String> options, String answer) {
        String wanted = answer.trim();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option != null && option.trim().equalsIgnoreCase(wanted)) {
                return i;
            }
        }
        return -1;
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
}
