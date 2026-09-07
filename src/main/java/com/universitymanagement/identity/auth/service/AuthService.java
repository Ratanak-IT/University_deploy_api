package com.universitymanagement.identity.auth.service;

import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.identity.auth.dto.request.*;
import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;
import com.universitymanagement.identity.auth.dto.response.RegisterResponse;
import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    String buildLoginUrl(String state, String codeChallenge, String redirectUri);

    LoginResponse exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    UserProfileResponse getProfile();
    UserDetailResponse getMyDetails();
    UserProfileResponse updateProfile(UpdateProfileRequest request);

    /**
     * Replaces the signed-in user's profile picture.
     *
     * <p>Lives here rather than on the student or teacher service because an
     * administrator is neither, and had no way to set one at all.
     */
    UserProfileResponse uploadMyAvatar(org.springframework.web.multipart.MultipartFile file);

    void changePassword(ChangePasswordRequest request);
}