package com.universitymanagement.admin.controller;


import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
import com.universitymanagement.admin.dto.request.UpdateStatusRequest;
import com.universitymanagement.admin.dto.response.LoginHistoryResponse;
import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.admin.dto.response.UserSummaryResponse;
import com.universitymanagement.admin.service.UserManageService;
import com.universitymanagement.identity.auth.dto.request.CreateUserRequest;
import com.universitymanagement.identity.auth.dto.request.UpdateUserRequest;
import com.universitymanagement.identity.auth.dto.response.CreateUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserManageService userManageService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public Page<UserSummaryResponse> findAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userManageService.findAllUsers(page, size);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}")
    public UserDetailResponse getUserDetails(@PathVariable String userId) {
        return userManageService.findUserById(userId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CreateUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userManageService.createUser(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{userId}")
    public UserDetailResponse updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return userManageService.updateUser(userId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{userId}/status")
    public void updateStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        userManageService.updateStatus(userId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable String userId) {
        userManageService.deleteUser(userId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{userId}/reset-password")
    public void resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        userManageService.resetPassword(userId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}/login-history")
    public List<LoginHistoryResponse> getLoginHistory(@PathVariable String userId) {
        return userManageService.getLoginHistory(userId);
    }
 //
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{userId}/soft-delete")
    public void softDeleteUser(@PathVariable String userId) {
        userManageService.softDeleteUser(userId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{userId}/restore")
    public void restoreUser(@PathVariable String userId) {
        userManageService.restoreUser(userId);
    }
}
