package com.universitymanagement.teacher.controller;


import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.department.service.DepartmentService;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.teacher.dto.request.AssignClassroomRequest;
import com.universitymanagement.teacher.dto.request.AssignSubjectRequest;
import com.universitymanagement.teacher.dto.request.CreateTeacherRequest;
import com.universitymanagement.teacher.dto.request.UpdateTeacherRequest;
import com.universitymanagement.teacher.dto.response.TeacherDetailResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import com.universitymanagement.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final DepartmentService departmentService;

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<TeacherResponse> getAllTeachers(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "25") int size) {
        return teacherService.getAllTeachers(page, size);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{teacherId}")
    public TeacherResponse getTeacherById(@PathVariable UUID teacherId) {
        return teacherService.getTeacherById(teacherId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public TeacherDetailResponse getTeacherByUserId(@PathVariable String userId) {
        return teacherService.findTeacherByUserId(userId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public TeacherResponse createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        return teacherService.createTeacher(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{teacherId}")
    public TeacherResponse updateTeacher(@PathVariable UUID teacherId,
                                         @Valid @RequestBody UpdateTeacherRequest request) {
        return teacherService.updateTeacher(teacherId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{teacherId}")
    public void deleteTeacher(@PathVariable UUID teacherId) {
        teacherService.deleteTeacher(teacherId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{teacherId}/classes")
    public List<ClassroomResponse> getAssignedClasses(@PathVariable UUID teacherId) {
        return teacherService.getAssignedClasses(teacherId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{teacherId}/assign-subject")
    public TeacherResponse assignSubject(@PathVariable UUID teacherId,
                                         @Valid @RequestBody AssignSubjectRequest request) {
        return teacherService.assignSubject(teacherId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{teacherId}/subjects/{subjectId}")
    public TeacherResponse unassignSubject(@PathVariable UUID teacherId,
                                           @PathVariable UUID subjectId) {
        return teacherService.unassignSubject(teacherId, subjectId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{departmentId}/teachers")
    public List<TeacherResponse> getTeachersByDepartment(@PathVariable UUID departmentId) {
        return departmentService.getTeachersByDepartment(departmentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{teacherId}/subjects")
    public List<SubjectResponse> getAssignedSubjects(@PathVariable UUID teacherId) {
        return teacherService.getAssignedSubjects(teacherId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{teacherId}/assign-classroom")
    public ClassroomResponse assignClassroom(@PathVariable UUID teacherId,
                                             @Valid @RequestBody AssignClassroomRequest request) {
        return teacherService.assignClassroom(teacherId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{teacherId}/assign-department/{departmentId}")
    public TeacherResponse assignDepartment(@PathVariable UUID teacherId,
                                            @PathVariable UUID departmentId) {
        return teacherService.assignDepartment(teacherId, departmentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{teacherId}/departments/{departmentId}")
    public TeacherResponse unassignDepartment(@PathVariable UUID teacherId,
                                              @PathVariable UUID departmentId) {
        return teacherService.unassignDepartment(teacherId, departmentId);
    }
}
