package com.universitymanagement.classroom.controller;

import com.universitymanagement.classroom.dto.request.AddStudentsRequest;
import com.universitymanagement.classroom.dto.request.AssignTeacherRequest;
import com.universitymanagement.classroom.dto.request.ClassroomCreateRequest;
import com.universitymanagement.classroom.dto.request.ClassroomUpdateRequest;
import com.universitymanagement.classroom.dto.response.ClassroomMemberResponse;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.dto.response.ClassroomStudentResponse;
import com.universitymanagement.classroom.service.ClassroomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomController {
    private final ClassroomService classroomService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ClassroomResponse> getAllClassrooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return classroomService.getAllClassrooms(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClassroomResponse createClassroom(@Valid @RequestBody ClassroomCreateRequest request) {
        return classroomService.createClassroom(request);
    }

    @PutMapping("/{classroomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassroomResponse updateClassroom(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomUpdateRequest request
    ) {
        return classroomService.updateClassroom(classroomId, request);
    }

    @PatchMapping("/{classroomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteClassroom(@PathVariable UUID classroomId) {
        classroomService.softDelete(classroomId);
    }

    @PostMapping("/{classroomId}/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClassroomMemberResponse addTeacher(
            @PathVariable UUID classroomId,
            @Valid @RequestBody AssignTeacherRequest request
    ) {
        return classroomService.addTeacherToClassroom(classroomId, request);
    }

    /** Teacher ទាំងអស់ក្នុង classroom. */
    @GetMapping("/{classroomId}/teachers")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<ClassroomMemberResponse> getTeachers(@PathVariable UUID classroomId) {
        return classroomService.getTeachersInClassroom(classroomId);
    }

    @DeleteMapping("/{classroomId}/teachers/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void removeTeacher(
            @PathVariable UUID classroomId,
            @PathVariable UUID teacherId
    ) {
        classroomService.removeTeacherFromClassroom(classroomId, teacherId);
    }

    @PatchMapping("/{classroomId}/lead-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassroomResponse setLeadTeacher(
            @PathVariable UUID classroomId,
            @Valid @RequestBody AssignTeacherRequest request
    ) {
        return classroomService.setLeadTeacher(classroomId, request);
    }


    @PostMapping("/{classroomId}/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public void addStudents(
            @PathVariable UUID classroomId,
            @Valid @RequestBody AddStudentsRequest request
    ) {
        classroomService.addStudentsToClassroom(classroomId, request);
    }

    @DeleteMapping("/{classroomId}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void removeStudent(
            @PathVariable UUID classroomId,
            @PathVariable UUID studentId
    ) {
        classroomService.removeStudentFromClassroom(classroomId, studentId);
    }

    @GetMapping("/{classroomId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ClassroomResponse getClassroomById(@PathVariable UUID classroomId) {
        return classroomService.getClassroomById(classroomId);
    }

    @GetMapping("/{classroomId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<ClassroomStudentResponse> getStudentsInClassroom(@PathVariable UUID classroomId) {
        return classroomService.getStudentsInClassroom(classroomId);
    }

    @GetMapping("/my-classrooms")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','ADMIN')")
    public List<ClassroomResponse> getMyClassrooms() {
        return classroomService.getMyClassrooms();
    }
}
