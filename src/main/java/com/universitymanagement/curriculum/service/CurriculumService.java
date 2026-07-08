package com.universitymanagement.curriculum.service;

import com.universitymanagement.curriculum.dto.request.CurriculumRequest;
import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CurriculumService {
    Page<CurriculumResponse> getAllCurriculums(int page, int size);
    Page<CurriculumResponse> getCurriculumsByProgram(UUID programId, int page, int size);
    CurriculumResponse createCurriculum( CurriculumRequest request);
    CurriculumResponse updateCurriculum(UUID curriculumId, CurriculumRequest request);
    void deleteCurriculum(UUID curriculumId);
    void softDelete(UUID curriculumId);
}
