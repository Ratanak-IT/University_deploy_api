package com.universitymanagement.curriculum.controller;

import com.universitymanagement.curriculum.dto.request.CurriculumRequest;
import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import com.universitymanagement.curriculum.service.CurriculumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curriculums")
@RequiredArgsConstructor
public class CurriculumController {
    private final CurriculumService curriculumService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public Page<CurriculumResponse> getAllCurriculums(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "25") int size){
        return curriculumService.getAllCurriculums(page, size);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/program/{programId}")
    public Page<CurriculumResponse> getCurriculumsByProgram(@PathVariable UUID programId,@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "25") int size){
        return curriculumService.getCurriculumsByProgram(programId, page, size);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CurriculumResponse createCurriculum(@Valid @RequestBody CurriculumRequest request){
        return curriculumService.createCurriculum(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{curriculumId}")
    public CurriculumResponse updateCurriculum(@PathVariable UUID curriculumId,@Valid @RequestBody CurriculumRequest request){
        return curriculumService.updateCurriculum(curriculumId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{curriculumId}")
    public void deleteCurriculum(@PathVariable UUID curriculumId){
        curriculumService.deleteCurriculum(curriculumId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{curriculumId}")
    public void softDelete(@PathVariable UUID curriculumId){
        curriculumService.softDelete(curriculumId);
    }

}
