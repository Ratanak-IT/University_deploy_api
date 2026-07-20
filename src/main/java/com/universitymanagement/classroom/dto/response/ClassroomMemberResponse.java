package com.universitymanagement.classroom.dto.response;

import com.universitymanagement.classroom.dto.ClassroomRole;
import com.universitymanagement.classroom.dto.MemberStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClassroomMemberResponse(
        UUID teacherId,

        String fullname,

        String email,

        ClassroomRole role,

        LocalDateTime joinedAt,

        MemberStatus status
) {
}
