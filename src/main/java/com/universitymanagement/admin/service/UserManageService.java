package com.universitymanagement.admin.service;

import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
import com.universitymanagement.admin.dto.request.UpdateStatusRequest;
import com.universitymanagement.admin.dto.response.AdminDetailResponse;
import com.universitymanagement.admin.dto.response.LoginHistoryResponse;
import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.admin.dto.response.UserSummaryResponse;
import com.universitymanagement.identity.auth.dto.request.CreateUserRequest;
import com.universitymanagement.identity.auth.dto.request.UpdateUserRequest;
import com.universitymanagement.identity.auth.dto.response.CreateUserResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserManageService {
    Page<UserSummaryResponse> findAllUsers(int page, int size);

    UserDetailResponse findUserById(String id);

    CreateUserResponse createUser(CreateUserRequest request);

    UserDetailResponse updateUser(String id, UpdateUserRequest request);

    void updateStatus(String id, UpdateStatusRequest request);

    void resetPassword(String id, AdminResetPasswordRequest request);

    List<LoginHistoryResponse> getLoginHistory(String id);

    void deleteUser(String id);

    AdminDetailResponse findAdminById(String id);

    void softDeleteUser(String id);

    void restoreUser(String userId);
}
