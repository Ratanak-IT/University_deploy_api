package com.universitymanagement.assignment.dto.response;

import java.util.UUID;

public record FileResponse(
        UUID fileId,
        String fileOriginalName,
        String previewUrl
) {
}
