package com.universitymanagement.student.service;

import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
import com.universitymanagement.student.dto.request.CreateStudentRequest;
import com.universitymanagement.student.dto.request.StudentUpdateProfileRequest;
import com.universitymanagement.student.dto.request.StudentUpdateRequest;
import com.universitymanagement.student.dto.response.StudentAdminResponse;
import com.universitymanagement.student.dto.response.StudentDetailResponse;
import com.universitymanagement.student.dto.response.StudentDirectoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StudentService {

    // for admin
    Page<StudentAdminResponse> getAllStudents(int page, int size, String keyword);

    /** Name/code search for teachers picking who to enroll — no admin-only fields. */
    Page<StudentDirectoryResponse> searchStudentDirectory(int page, int size, String keyword);

    /** Withdrawn students, for the screen that undoes a removal. */
    Page<StudentAdminResponse> getWithdrawnStudents(int page, int size);

    /**
     * Puts a withdrawn student back.
     *
     * <p>Nothing was destroyed on removal, so this is only a matter of clearing
     * the flag and re-enabling the sign-in account.
     */
    void restoreStudent(UUID studentId);

    StudentAdminResponse getStudentById(UUID studentId);

    StudentAdminResponse createStudent(CreateStudentRequest request);

    StudentAdminResponse updateStudent(UUID studentId, StudentUpdateRequest request);

    void deleteStudent(UUID studentId);

    StudentDetailResponse getMyProfile();

    StudentDetailResponse updateMyProfile(StudentUpdateProfileRequest request);
    StudentDetailResponse findStudentById(String keycloakUserId);
    StudentDetailResponse uploadMyAvatar(MultipartFile file);
    StudentAdminResponse assignAdvisor(UUID studentId, UUID teacherId);

    /**
     * Sets a new password on behalf of someone who cannot supply their old one.
     *
     * <p>No current password is asked for, and that is the point: the person
     * who has forgotten theirs is by definition unable to produce it. The
     * authority comes from the caller being an administrator, which the
     * endpoint enforces.
     *
     * <p>Takes the student id rather than the Keycloak one, because that is what the
     * admin screens hold — and putting Keycloak ids into list payloads just so
     * a button can use them would widen what those payloads expose for no gain.
     */
    void resetPassword(UUID studentId, AdminResetPasswordRequest request);
}
