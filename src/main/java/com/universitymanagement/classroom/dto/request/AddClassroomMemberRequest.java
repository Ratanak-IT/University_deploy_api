package com.universitymanagement.classroom.dto.request;

import com.universitymanagement.classroom.dto.ClassroomRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddClassroomMemberRequest(
        @NotNull
        UUID userId,

        @NotNull
        ClassroomRole role

) {
}
