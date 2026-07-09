package com.universitymanagement.classroom.dto.request;

import java.util.List;
import java.util.UUID;

public record AddStudentsRequest(
        List<UUID> studentIds
) {
}
