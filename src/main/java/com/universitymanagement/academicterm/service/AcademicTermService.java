package com.universitymanagement.academicterm.service;

import com.universitymanagement.academicterm.dto.request.AcademicTermRequest;
import com.universitymanagement.academicterm.dto.response.AcademicTermResponse;

import java.util.List;
import java.util.UUID;

public interface AcademicTermService {
    AcademicTermResponse create(AcademicTermRequest request);

    AcademicTermResponse update(UUID termId, AcademicTermRequest request);

    List<AcademicTermResponse> getAll();

    AcademicTermResponse getById(UUID termId);

    void delete(UUID termId);
}
