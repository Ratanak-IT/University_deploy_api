package com.universitymanagement.certificate.service;

import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.repository.IssuedCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Year;

/**
 * Reference numbers and verification codes.
 *
 * <p>The number is readable and ordered — UT-2026-DEGREE-00042 — because it
 * ends up quoted in letters and phone calls. The verification code is random,
 * because a guessable one would let anyone enumerate the register.
 */
@Component
@RequiredArgsConstructor
public class CertificateNumberGenerator {

    private static final String PREFIX = "UT";
    /** No I, O, 0 or 1 — these are read aloud and typed by hand. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IssuedCertificateRepository issuedRepository;

    public synchronized String nextNumber(CertificateType type) {
        int year = Year.now().getValue();
        String pattern = PREFIX + "-" + year + "-" + type.name() + "-%";
        long used = issuedRepository.countForNumbering(type, pattern);

        return "%s-%d-%s-%05d".formatted(PREFIX, year, type.name(), used + 1);
    }

    public String newVerificationCode() {
        StringBuilder code = new StringBuilder(14);
        for (int group = 0; group < 3; group++) {
            if (group > 0) {
                code.append('-');
            }
            for (int i = 0; i < 4; i++) {
                code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return code.toString();
    }
}
