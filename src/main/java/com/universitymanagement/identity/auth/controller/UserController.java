package com.universitymanagement.identity.auth.controller;

import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
import com.universitymanagement.admin.dto.request.UpdateStatusRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManageService userManageService;

    @GetMapping
    public ResponseEntity<Page<UserSummaryResponse>> findAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userManageService.findAllUsers(page, size));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> getUserDetails(@PathVariable String userId) {
        return ResponseEntity.ok(userManageService.findUserById(userId));
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = userManageService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userManageService.updateUser(userId, request));
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<Map<String, String>> assignRole(
            @PathVariable String userId,
            @RequestParam(required = false) String roleName,
            @RequestBody(required = false) Map<String, String> body) {
        String roleToAssign = roleName;
        if (roleToAssign == null && body != null) {
            roleToAssign = body.get("roleName");
            if (roleToAssign == null) {
                roleToAssign = body.get("role");
            }
        }
        if (roleToAssign == null || roleToAssign.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roleName parameter or body property is required"));
        }
        userManageService.assignRole(userId, roleToAssign);
        return ResponseEntity.ok(Map.of("message", "Role " + roleToAssign + " assigned successfully to user " + userId));
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<Map<String, String>> updateRole(
            @PathVariable String userId,
            @RequestParam(required = false) String roleName,
            @RequestBody(required = false) Map<String, String> body) {
        return assignRole(userId, roleName, body);
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        userManageService.updateStatus(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        userManageService.resetPassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userManageService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
