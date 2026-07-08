package com.universitymanagement.subject.service;

import com.universitymanagement.subject.dto.request.SubjectRequest;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface SubjectService {

    Page<SubjectResponse> getAllSubjects(int page, int size);
    SubjectResponse getSubjectById(UUID subjectId);
    SubjectResponse createSubject(SubjectRequest subjectRequest);
    SubjectResponse updateSubject(UUID subjectId, SubjectRequest subjectRequest);
    void deleteSubject(UUID subjectId);
    void softDelete(UUID subjectId);
}
