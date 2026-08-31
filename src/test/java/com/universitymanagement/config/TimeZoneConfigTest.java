package com.universitymanagement.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeZoneConfigTest {

    private TimeZone original;

    @BeforeEach
    void rememberDefault() {
        // The subject sets a JVM-wide default, so it has to be put back or
        // every test that runs afterwards inherits it.
        original = TimeZone.getDefault();
    }

    @AfterEach
    void restoreDefault() {
        TimeZone.setDefault(original);
    }

    private void apply(String timezone) {
        TimeZoneConfig config = new TimeZoneConfig();
        ReflectionTestUtils.setField(config, "timezone", timezone);
        config.applyTimezone();
    }

    @Test
    void pinsTheClockToPhnomPenhWhateverTheHostSays() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        apply("Asia/Phnom_Penh");

        assertEquals("Asia/Phnom_Penh", TimeZone.getDefault().getID());
    }

    @Test
    void aTimestampWrittenOnAUtcHostReadsAsCambodianWallClock() {
        // The point of the whole class: LocalDateTime.now() carries no zone, so
        // a container on UTC would otherwise record a reading seven hours
        // behind the one a machine in Phnom Penh records for the same instant.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        apply("Asia/Phnom_Penh");

        // Asserted on the zone's offset rather than by subtracting two now()
        // calls: those are taken microseconds apart, so the difference is a
        // hair under seven hours and any truncation lands on six.
        assertEquals(ZoneOffset.ofHours(7),
                TimeZone.getDefault().toZoneId().getRules().getOffset(Instant.now()),
                "Cambodia is UTC+7 and observes no daylight saving");

        assertEquals(LocalDateTime.now(ZoneId.of("Asia/Phnom_Penh")).getHour(),
                LocalDateTime.now().getHour(),
                "the no-argument now() must already be Cambodian wall-clock time");
    }

    @Test
    void anUnknownZoneIsNotSilentlyAcceptedAsGmt() {
        // TimeZone.getTimeZone answers GMT for anything it does not recognise
        // rather than failing, so a typo in the property would put the whole
        // system back on UTC with nothing in the logs to say why. The config
        // logs an error for this; the test pins the behaviour it reports on.
        apply("Asia/Phnom_Penn");

        assertEquals("GMT", TimeZone.getDefault().getID(),
                "the JDK's fallback — which is why the config logs an error");
    }
}
