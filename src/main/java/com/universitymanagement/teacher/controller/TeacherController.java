package com.universitymanagement.teacher.controller;


import com.universitymanagement.admin.dto.response.AdminDetailResponse;
import com.universitymanagement.teacher.dto.response.TeacherDetailResponse;
import com.universitymanagement.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}")
    public TeacherDetailResponse getAdminDetails(@PathVariable String userId) {
        return teacherService.findTeacherById(userId);
    }

}
