package com.universitymanagement.lesson.controller;

import com.universitymanagement.lesson.dto.request.LessonRequest;
import com.universitymanagement.lesson.dto.response.LessonResponse;
import com.universitymanagement.lesson.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping(
            value = "/classrooms/{classroomId}/lessons",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public LessonResponse createLesson(
            @PathVariable UUID classroomId,
            @Valid @RequestPart("lesson") LessonRequest request,
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return lessonService.createLesson(classroomId, request, files);
    }

    @PutMapping(
            value = "/lessons/{lessonId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public LessonResponse updateLesson(
            @PathVariable UUID lessonId,
            @Valid @RequestPart("lesson") LessonRequest request,
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return lessonService.updateLesson(lessonId, request, files);
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void deleteLesson(@PathVariable UUID lessonId) {
        lessonService.deleteLesson(lessonId);
    }

    @GetMapping("/classrooms/{classroomId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public List<LessonResponse> getLessonsByClassroom(@PathVariable UUID classroomId) {
        return lessonService.getLessonsByClassroom(classroomId);
    }


    @DeleteMapping("/lessons/{lessonId}/files/{fileId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public LessonResponse removeLessonFile(
            @PathVariable UUID lessonId,
            @PathVariable UUID fileId
    ) {
        return lessonService.removeLessonFile(lessonId, fileId);
    }
}
