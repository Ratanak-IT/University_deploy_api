package com.universitymanagement.quiz.migration;

import com.universitymanagement.quiz.entity.Quiz;
import com.universitymanagement.quiz.entity.QuizAssignment;
import com.universitymanagement.quiz.repository.QuizAssignmentRepository;
import com.universitymanagement.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves each quiz's single {@code classroom_id} into the releases table, once,
 * on the first start after multi-classroom assignment ships.
 *
 * <p>The old column is left in place rather than dropped: it is what this
 * migration reads, and keeping it means a rollback does not lose which section
 * a quiz belonged to.
 */
@Component
@RequiredArgsConstructor
@Order(110)
@Slf4j
public class QuizClassroomMigration implements ApplicationRunner {

    private final QuizRepository quizRepository;
    private final QuizAssignmentRepository assignmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (assignmentRepository.count() > 0) {
            return; // already migrated, or releases are already in use
        }

        int migrated = 0;
        for (Quiz quiz : quizRepository.findAll()) {
            if (quiz.getClassroom() == null || Boolean.TRUE.equals(quiz.getIsDeleted())) {
                continue;
            }

            QuizAssignment assignment = new QuizAssignment();
            assignment.setQuiz(quiz);
            assignment.setClassroom(quiz.getClassroom());
            assignmentRepository.save(assignment);
            migrated++;
        }

        if (migrated > 0) {
            log.info("Quiz classroom migration: {} quiz(zes) moved onto releases.", migrated);
        }
    }
}
