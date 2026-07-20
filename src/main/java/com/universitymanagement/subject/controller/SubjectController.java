package com.universitymanagement.subject.controller;

import com.universitymanagement.subject.dto.request.SubjectRequest;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public Page<SubjectResponse> getAllSubjects(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "25") int size){
        return subjectService.getAllSubjects(page, size);
    }
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{subjectId}")
    public SubjectResponse getSubjectById(@PathVariable UUID subjectId){
        return subjectService.getSubjectById(subjectId);
    }
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SubjectResponse createSubject(@Valid @RequestBody SubjectRequest subjectRequest){
        return subjectService.createSubject(subjectRequest);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{subjectId}")
    public SubjectResponse updateSubject(@PathVariable UUID subjectId, @Valid @RequestBody SubjectRequest subjectRequest){
        return subjectService.updateSubject(subjectId, subjectRequest);
    }

    @DeleteMapping("/delete/{subjectId}")
    public void deleteSubject(@PathVariable UUID subjectId){
        subjectService.deleteSubject(subjectId);
    }

    @PatchMapping("/{subjectId}")
    public void softDelete(@PathVariable UUID subjectId){
        subjectService.softDelete(subjectId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{subjectId}/classrooms/count")
    public long countClassroomsBySubject(@PathVariable UUID subjectId){
        return subjectService.countClassroomsBySubject(subjectId);
    }
}
