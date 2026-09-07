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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KeycloakProperties keycloakProperties;

    @Value("${app.frontend-url:https://cambodiaunm.vercel.app}")
    private String frontendUrl;

    /** Where the admin portal's own `/auth/callback` lives — a separate app/origin from {@link #frontendUrl}. */
    @Value("${app.admin-frontend-url:https://administratorcambodiaunm-three.vercel.app}")
    private String adminFrontendUrl;

    private static final String SESSION_CODE_VERIFIER = "PKCE_CODE_VERIFIER";
    private static final String SESSION_STATE = "OAUTH2_STATE";
    private static final String SESSION_REDIRECT_URI = "OAUTH2_REDIRECT_URI";
    private static final String SESSION_APP = "LOGIN_APP";
    private static final String SESSION_RETURN_TO = "LOGIN_RETURN_TO";

    /**
     * Front ends this server is willing to hand tokens to.
     *
     * <p>The callback finishes by putting the access and refresh tokens in a URL
     * fragment. Redirecting to an address supplied in the query string without
     * checking it would therefore be an open redirect that leaks credentials:
     * anyone could send a victim to /auth/login?returnTo=their-own-site and
     * collect the tokens on arrival. An allow-list is what makes the parameter
     * safe to accept at all — the same reason OAuth registers redirect URIs
     * rather than trusting whatever a request asks for.
     */
    @Value("${app.allowed-return-urls:}")
    private String allowedReturnUrls;

//    @PostMapping("/register")
//    public RegisterResponse register(
//            @Valid @RequestBody RegisterRequest request
//    ) {
//        log.info("Controller reached");
//        return authService.register(request);
//    }


    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    @GetMapping("/login")
    public void login(
            @RequestParam(value = "app", required = false) String app,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        PkceUtil.Pkce pkce = PkceUtil.generate();
        String redirectUri = resolveCallbackUri(request);

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_CODE_VERIFIER, pkce.codeVerifier());
        session.setAttribute(SESSION_STATE, pkce.state());
        session.setAttribute(SESSION_REDIRECT_URI, redirectUri);
        session.setAttribute(SESSION_APP, app);

        // Lets a developer running the site locally finish login on their own
        // machine while still talking to this server. Stored only if it is on
        // the allow-list; anything else is dropped and the default is used.
        String allowed = permittedReturnUrl(returnTo);
        if (allowed != null) {
            session.setAttribute(SESSION_RETURN_TO, allowed);
            log.info("Login will return to {}", allowed);
        }

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
    public void callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new InvalidAuthorizationCodeException("No active login session. Please start login again.");
        }

        String expectedState = (String) session.getAttribute(SESSION_STATE);
        String codeVerifier = (String) session.getAttribute(SESSION_CODE_VERIFIER);
        String redirectUri = (String) session.getAttribute(SESSION_REDIRECT_URI);
        String app = (String) session.getAttribute(SESSION_APP);

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
        session.removeAttribute(SESSION_APP);
        session.removeAttribute(SESSION_RETURN_TO);

        // Tokens travel in the URL fragment, not a query string: the fragment
        // never leaves the browser (not sent to any server, not logged by
        // this redirect or the frontend's own server), and the frontend page
        // that reads it strips it from the address bar immediately after.
        String fragment = "access_token=" + urlEncode(loginResponse.getAccessToken())
                + "&refresh_token=" + urlEncode(loginResponse.getRefreshToken())
                + "&token_type=" + urlEncode(loginResponse.getTokenType())
                + "&expires_in=" + loginResponse.getExpiresIn();

        // The admin portal is a separate app/origin from the student/teacher
        // one, so it needs its own callback URL. `app=admin` on the initial
        // /login request (see below) is what threads that choice through the
        // whole Keycloak round trip via the session, since nothing else
        // survives the redirect to Keycloak and back.
        String returnTo = (String) session.getAttribute(SESSION_RETURN_TO);
        String targetFrontend = returnTo != null
                ? returnTo
                : ("admin".equals(app) ? adminFrontendUrl : frontendUrl);

        response.sendRedirect(targetFrontend + "/auth/callback#" + fragment);
    }

    /**
     * @return the requested return URL if it is allowed, otherwise null
     */
    private String permittedReturnUrl(String requested) {
        if (requested == null || requested.isBlank() || allowedReturnUrls == null) {
            return null;
        }

        String candidate = requested.trim();
        // Compared whole and exactly. Matching on a prefix would accept
        // "http://localhost:3000.evil.com", which starts with an allowed value
        // and is a different site entirely.
        for (String allowed : allowedReturnUrls.split(",")) {
            String trimmed = allowed.trim();
            if (!trimmed.isEmpty() && trimmed.equals(candidate)) {
                return trimmed;
            }
        }

        log.warn("Refused returnTo that is not on the allow-list: {}", candidate);
        return null;
    }

    private static String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
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

    /**
     * Ends the login properly, then sends the browser home.
     *
     * <p>Clearing localStorage only forgets the tokens on this device. The
     * Keycloak SSO cookie lives on the Keycloak domain and survives it, so the
     * next login request is answered silently from that session and the user
     * appears to be signed straight back in without ever seeing a form. Ending
     * the session has to be done by Keycloak, which is what this hands over to.
     *
     * <p>Paired with {@code POST /logout}, which revokes the refresh token: the
     * token goes in a request body there rather than a query string here,
     * because a query string would be written to access logs, browser history
     * and every proxy in between.
     */
    @GetMapping("/logout")
    public void endSession(
            @RequestParam(value = "app", required = false) String app,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Our own session holds the PKCE verifier and OAuth state. Left alive,
        // a half-finished login could be resumed after signing out.
        //
        // Read before invalidating: the session is also where login recorded
        // which app started it, and destroying it first would lose that.
        HttpSession session = request.getSession(false);
        String startedBy = session != null ? (String) session.getAttribute(SESSION_APP) : null;
        if (session != null) {
            session.invalidate();
        }

        // The admin portal is a separate origin, so sending it back to the
        // student app would strand whoever signed out of it. The query
        // parameter wins because the session may already be gone — signing out
        // twice, or after it expired, still has to land somewhere sensible.
        String which = app != null ? app : startedBy;
        String postLogoutRedirect = "admin".equals(which) ? adminFrontendUrl : frontendUrl;

        String endSession = keycloakProperties.getServerUrl()
                + "/realms/" + keycloakProperties.getTargetRealm()
                + "/protocol/openid-connect/logout"
                + "?client_id=" + urlEncode(keycloakProperties.getClientId())
                + "&post_logout_redirect_uri=" + urlEncode(postLogoutRedirect);

        log.info("Ending Keycloak session, returning to {}", postLogoutRedirect);
        response.sendRedirect(endSession);
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

    /**
     * Replaces the signed-in user's profile picture.
     *
     * <p>Open to any signed-in role. Students and teachers each had their own
     * version of this on their own controller; an administrator is neither, and
     * so had no way to set a picture at all.
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadMyAvatar(@RequestPart("file") MultipartFile file) {
        return authService.uploadMyAvatar(file);
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