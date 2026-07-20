package com.universitymanagement.lesson.service;

import com.universitymanagement.lesson.dto.request.LessonRequest;
import com.universitymanagement.lesson.dto.response.FileStreamResult;
import com.universitymanagement.lesson.dto.response.LessonResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    LessonResponse createLesson(UUID classroomId, LessonRequest request, List<MultipartFile> files);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request, List<MultipartFile> files);

    void removeLessonFile(UUID lessonId, UUID fileId);

    void deleteLesson(UUID lessonId);

    List<LessonResponse> getLessonsByClassroom(UUID classroomId);
    FileStreamResult getLessonFilePreview(UUID lessonId, UUID fileId);

}
