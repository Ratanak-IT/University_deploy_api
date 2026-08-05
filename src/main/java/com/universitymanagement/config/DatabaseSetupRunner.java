package com.universitymanagement.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSetupRunner {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void run() {
        try {
            log.info("Executing database alter statements to support stand-alone lesson & assignment templates...");
            jdbcTemplate.execute("ALTER TABLE lessons ALTER COLUMN classroom_id DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE assignments ALTER COLUMN classroom_id DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE assignments ALTER COLUMN due_date DROP NOT NULL");
            log.info("Database alter statements executed successfully.");
        } catch (Exception e) {
            log.warn("Database alter execution skipped or failed: " + e.getMessage());
        }
    }
}
