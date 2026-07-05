package com.universitymanagement.identity.auth.service;

import com.universitymanagement.identity.auth.dto.request.*;
import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;
import com.universitymanagement.identity.auth.dto.response.RegisterResponse;
import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse exchangeAuthorizationCode(String code, String codeVerifier);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    UserProfileResponse getProfile();

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);
}