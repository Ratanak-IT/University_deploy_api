package com.universitymanagement.lesson.service;

import com.universitymanagement.lesson.dto.request.LessonRequest;
import com.universitymanagement.lesson.dto.response.LessonResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    LessonResponse createLesson(UUID classroomId, LessonRequest request, MultipartFile file);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request, MultipartFile file);

    void deleteLesson(UUID lessonId);

    List<LessonResponse> getLessonsByClassroom(UUID classroomId);
}
