package com.universitymanagement.program.service;

import com.universitymanagement.program.dto.request.ProgramRequest;
import com.universitymanagement.program.dto.response.ProgramResponse;
import com.universitymanagement.student.dto.response.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProgramService {
    ProgramResponse create(ProgramRequest request);

    ProgramResponse getById(UUID id);

    Page<ProgramResponse> getAll(Integer page, Integer size);

    ProgramResponse update(UUID id, ProgramRequest request);

    void delete(UUID id);
    List<StudentResponse> getStudentsByProgram(UUID programId);
}
