package com.universitymanagement.student.controller;


import com.universitymanagement.student.dto.response.StudentDetailResponse;
import com.universitymanagement.student.service.StudentService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}")
    public StudentDetailResponse getStudentDetails(@PathVariable String userId) {
        return studentService.findStudentById(userId);
    }
}
