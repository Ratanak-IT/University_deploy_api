package com.universitymanagement.identity.auth.controller;

import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.identity.auth.dto.request.*;
import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;
import com.universitymanagement.identity.auth.dto.response.RegisterResponse;
import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;
import com.universitymanagement.identity.auth.service.AuthService;
import com.universitymanagement.identity.auth.keycloak.config.KeycloakProperties;
import com.universitymanagement.identity.exception.InvalidAuthorizationCodeException;
import com.universitymanagement.identity.util.PkceUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KeycloakProperties keycloakProperties;

    private static final String SESSION_CODE_VERIFIER = "PKCE_CODE_VERIFIER";
    private static final String SESSION_STATE = "OAUTH2_STATE";
    private static final String SESSION_REDIRECT_URI = "OAUTH2_REDIRECT_URI";

//    @PostMapping("/register")
//    public RegisterResponse register(
//            @Valid @RequestBody RegisterRequest request
//    ) {
//        log.info("Controller reached");
//        return authService.register(request);
//    }


    /**
     * ចាប់ផ្ដើម login៖ redirect browser ទៅ Keycloak login form
     * (Authorization Code + PKCE flow)។ User login នៅលើ form របស់ Keycloak ផ្ទាល់។
     */

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PkceUtil.Pkce pkce = PkceUtil.generate();
        String redirectUri = resolveCallbackUri(request);

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_CODE_VERIFIER, pkce.codeVerifier());
        session.setAttribute(SESSION_STATE, pkce.state());
        session.setAttribute(SESSION_REDIRECT_URI, redirectUri);

        String authorizationUrl = authService.buildLoginUrl(pkce.state(), pkce.codeChallenge(), redirectUri);
        log.info("Redirecting to Keycloak login form: {}", authorizationUrl);

        response.sendRedirect(authorizationUrl);
    }

    private String resolveCallbackUri(HttpServletRequest request) {
        String configured = keycloakProperties.getRedirectUri();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);

        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        if (!defaultPort && port > 0) {
            sb.append(":").append(port);
        }
        sb.append(request.getContextPath()).append("/api/v1/auth/callback");
        return sb.toString();
    }

    /**
     * Callback endpoint ដែល Keycloak redirect មកវិញ បន្ទាប់ពី user login រួច។
     * ត្រូវផ្គូផ្គងនឹង keycloak.redirect-uri នៅក្នុង application.yaml
     * និង "Valid redirect URIs" នៅក្នុង Keycloak client។
     */
    @GetMapping("/callback")
    public LoginResponse callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new InvalidAuthorizationCodeException("No active login session. Please start login again.");
        }

        String expectedState = (String) session.getAttribute(SESSION_STATE);
        String codeVerifier = (String) session.getAttribute(SESSION_CODE_VERIFIER);
        String redirectUri = (String) session.getAttribute(SESSION_REDIRECT_URI);

        if (expectedState == null || !expectedState.equals(state)) {
            throw new InvalidAuthorizationCodeException("Invalid OAuth2 state (possible CSRF or expired session).");
        }
        if (codeVerifier == null || redirectUri == null) {
            throw new InvalidAuthorizationCodeException("Missing PKCE verifier / redirect URI. Please start login again.");
        }

        LoginResponse loginResponse = authService.exchangeAuthorizationCode(code, codeVerifier, redirectUri);

        session.removeAttribute(SESSION_STATE);
        session.removeAttribute(SESSION_CODE_VERIFIER);
        session.removeAttribute(SESSION_REDIRECT_URI);

        return loginResponse;
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authService.refreshToken(request);
    }

    @PostMapping("/logout")
    public void logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me/details")
    public UserDetailResponse getMyDetails() {
        return authService.getMyDetails();
    }

    @GetMapping("/me")
    public UserProfileResponse getProfile() {
        return authService.getProfile();
    }

    @PutMapping("/me/profile")
    public UserProfileResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateProfile(request);
    }

    @PutMapping("/change-password")
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(request);
    }
}