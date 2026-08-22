package com.universitymanagement.grading.service;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.grading.dto.request.SaveGradeSchemeRequest;
import com.universitymanagement.grading.dto.response.GradeComponentResponse;
import com.universitymanagement.grading.entity.GradeComponent;

import java.util.List;
import java.util.UUID;

public interface GradeSchemeService {

    List<GradeComponentResponse> getScheme(UUID classroomId);

    List<GradeComponentResponse> saveScheme(UUID classroomId, SaveGradeSchemeRequest request);

    /**
     * Guarantees the classroom has a policy, seeding the institutional default
     * the first time its gradebook is opened. Returns the components in order.
     */
    List<GradeComponent> ensureScheme(Classroom classroom);
}
