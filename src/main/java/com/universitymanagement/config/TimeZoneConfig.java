package com.universitymanagement.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Pins the application clock to one timezone, wherever it runs.
 *
 * <p>Every timestamp in this system is a {@code LocalDateTime} — a wall-clock
 * reading with no zone attached. What that reading means therefore depends
 * entirely on the JVM's default zone, which is inherited from the host: a
 * Windows laptop in Phnom Penh gives UTC+7, while an Alpine container gives
 * UTC. The same line of code writes times seven hours apart depending on where
 * it happens to run, and nothing in the stored value records which was meant.
 *
 * <p>That is not hypothetical here. A certificate issued through the deployed
 * API and one issued from a developer's machine were being recorded seven hours
 * apart, and a transcript would print whichever the row happened to hold.
 *
 * <p>Setting it in code rather than through {@code TZ} or a {@code -D} flag
 * means it holds for {@code bootRun}, the container and the test suite alike,
 * with no way to deploy an environment that quietly forgot it.
 */
@Slf4j
@Configuration
public class TimeZoneConfig {

    @Value("${app.timezone:Asia/Phnom_Penh}")
    private String timezone;

    @PostConstruct
    public void applyTimezone() {
        TimeZone zone = TimeZone.getTimeZone(timezone);

        // getTimeZone falls back to GMT for an unknown id rather than failing,
        // so a typo would silently put the whole system back on UTC.
        if (!zone.getID().equals(timezone)) {
            log.error("Unknown timezone '{}' — falling back to {}. Times will be wrong.",
                    timezone, zone.getID());
        }

        TimeZone.setDefault(zone);
        log.info("Application timezone set to {} (offset {})",
                zone.getID(), zone.toZoneId().getRules().getOffset(java.time.Instant.now()));
    }
}
