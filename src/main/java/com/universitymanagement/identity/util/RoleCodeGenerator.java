package com.universitymanagement.identity.util;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

@Component
public class RoleCodeGenerator {

    public String generate(String prefix) {
        String year = String.valueOf(Year.now().getValue());
        String random = UUID.randomUUID().toString()
                .substring(0, 6)
                .toUpperCase();
        return prefix + "-" + year + "-" + random;
    }
}