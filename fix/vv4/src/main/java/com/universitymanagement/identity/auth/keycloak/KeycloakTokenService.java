package com.universitymanagement.identity.auth.keycloak;

import com.universitymanagement.identity.auth.dto.request.LoginRequest;
import com.universitymanagement.identity.auth.dto.request.LogoutRequest;
import com.universitymanagement.identity.auth.dto.request.RefreshTokenRequest;
import com.universitymanagement.identity.auth.dto.request.RegisterRequest;
import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;

public interface KeycloakTokenService {

    LoginResponse login(String email, String password);

    LoginResponse exchangeAuthorizationCode(
            String code,
            String codeVerifier);

    RefreshTokenResponse refreshToken(
            RefreshTokenRequest request);

    void logout(LogoutRequest request);

}