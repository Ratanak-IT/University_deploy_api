package com.universitymanagement.score.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamScoreSchemaFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Checking and dropping legacy exam_scores_exam_type_check constraint...");
            jdbcTemplate.execute("ALTER TABLE exam_scores DROP CONSTRAINT IF EXISTS exam_scores_exam_type_check");
            log.info("Successfully dropped legacy constraint exam_scores_exam_type_check.");
        } catch (Exception e) {
            log.warn("Could not drop legacy constraint exam_scores_exam_type_check: {}", e.getMessage());
        }
    }
}
