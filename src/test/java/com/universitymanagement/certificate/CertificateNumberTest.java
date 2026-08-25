package com.universitymanagement.certificate;

import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.repository.IssuedCertificateRepository;
import com.universitymanagement.certificate.service.CertificateNumberGenerator;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CertificateNumberTest {

    private final IssuedCertificateRepository repository = mock(IssuedCertificateRepository.class);
    private final CertificateNumberGenerator generator = new CertificateNumberGenerator(repository);

    @Test
    void numbersAreReadableAndRunInSequence() {
        when(repository.countForNumbering(any(), anyString())).thenReturn(41L);

        String number = generator.nextNumber(CertificateType.DEGREE);

        assertEquals("UT-%d-DEGREE-00042".formatted(Year.now().getValue()), number,
                "the number is quoted in letters and read over the phone, so it "
                        + "has to be legible rather than a UUID");
    }

    @Test
    void theFirstOfAYearStartsAtOne() {
        when(repository.countForNumbering(any(), anyString())).thenReturn(0L);

        assertTrue(generator.nextNumber(CertificateType.COMPLETION).endsWith("-00001"));
    }

    @Test
    void eachTypeIsNumberedSeparately() {
        when(repository.countForNumbering(any(), anyString())).thenReturn(5L);

        assertTrue(generator.nextNumber(CertificateType.DEGREE).contains("-DEGREE-"));
        assertTrue(generator.nextNumber(CertificateType.TRANSCRIPT).contains("-TRANSCRIPT-"));
    }

    @Test
    void verificationCodesAreRandomNotSequential() {
        // A guessable code would let anyone walk the register by trying values.
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(generator.newVerificationCode());
        }

        assertTrue(seen.size() > 495,
                "codes must not collide in any realistic volume; got " + seen.size() + " of 500");
    }

    @Test
    void verificationCodesAvoidCharactersThatAreMisread() {
        Pattern shape = Pattern.compile("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}");

        for (int i = 0; i < 200; i++) {
            String code = generator.newVerificationCode();
            assertTrue(shape.matcher(code).matches(), "unexpected shape: " + code);
            // These are typed off paper, where 0/O and 1/I are indistinguishable.
            assertFalse(code.contains("0"), code);
            assertFalse(code.contains("O"), code);
            assertFalse(code.contains("1"), code);
            assertFalse(code.contains("I"), code);
        }
    }
}
