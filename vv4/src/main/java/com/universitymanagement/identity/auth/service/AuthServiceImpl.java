package com.universitymanagement.identity.auth.service;

import com.universitymanagement.identity.auth.dto.request.*;
import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;
import com.universitymanagement.identity.auth.dto.response.RegisterResponse;
import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;
import com.universitymanagement.identity.auth.mapper.AuthMapper;
import com.universitymanagement.identity.auth.mapper.UserMapper;
import com.universitymanagement.identity.entity.AccountStatus;
import com.universitymanagement.identity.entity.RefreshToken;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.DuplicateResourceException;
import com.universitymanagement.identity.exception.InvalidCredentialsException;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.auth.keycloak.KeycloakAdminService;
import com.universitymanagement.identity.auth.keycloak.KeycloakTokenService;
import com.universitymanagement.identity.repository.RefreshTokenRepository;
import com.universitymanagement.identity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakTokenService keycloakTokenService;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthServiceImpl(UserRepository userRepository, AuthMapper authMapper, KeycloakAdminService keycloakAdminService, KeycloakTokenService keycloakTokenService, UserMapper userMapper, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.authMapper = authMapper;
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakTokenService = keycloakTokenService;
        this.userMapper = userMapper;
        this.refreshTokenRepository = refreshTokenRepository;
    }


    @Override
    public RegisterResponse register(RegisterRequest request) {
        // Step 1. Validate business rules
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password not match");
        }

        // Step 2. Check duplicate email

        if(userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists.");
        }
        // Step 3. Create Keycloak user
        String keycloakUserId = keycloakAdminService.createUser(request);
        log.info("Keycloak user created successfully. id={}", keycloakUserId);

        // Step 4. Assign role defualt role
        keycloakAdminService.assignRole(keycloakUserId, "STUDENT");
        log.info("Role assigned successfully.");

        log.debug("Mapping request to User entity...");

        // Step 5. Save local database

        User user  = userMapper.toEntity(request);

        // 6. Set fields not present in request
        user.setKeycloakId(keycloakUserId);
        user.setAccountStatus(String.valueOf(AccountStatus.valueOf("ACTIVE")));
        user.setIsActive(true);



        // 7. Save
        User savedUser = userRepository.save(user);

        log.info("User saved successfully. id={}", savedUser.getId());


        // 8. Return response
        return userMapper.toRegisterResponse(savedUser);
    }

    @Override
    public LoginResponse exchangeAuthorizationCode(String code, String codeVerifier) {
        return keycloakTokenService.exchangeAuthorizationCode(code, codeVerifier);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LoginResponse response = keycloakTokenService.login(request.getEmail(), request.getPassword());
        recordSession(request.getEmail(), response);
        return response;
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenResponse response = keycloakTokenService.refreshToken(request);

        refreshTokenRepository.findByRefreshTokenHash(hash(request.refreshToken()))
                .ifPresent(token -> {
                    token.setRefreshTokenHash(hash(response.getRefreshToken()));
                    token.setLastRefreshed(LocalDateTime.now());
                    if (response.getExpiresIn() != null) {
                        token.setExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresIn()));
                    }
                    refreshTokenRepository.save(token);
                });

        return response;
    }

    @Override
    public void logout(LogoutRequest request) {
        keycloakTokenService.logout(request);

        if (request.getRefreshToken() != null) {
            refreshTokenRepository.findByRefreshTokenHash(hash(request.getRefreshToken()))
                    .ifPresent(token -> {
                        token.setIsActive(false);
                        token.setRevoked(true);
                        token.setRevokeReason("USER_LOGOUT");
                        token.setLogoutTime(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    });
        }
    }

    @Override
    public UserProfileResponse getProfile() {
        return userMapper.toResponse(getCurrentUser());
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        boolean emailChanged = request.getEmail() != null
                && !request.getEmail().equalsIgnoreCase(user.getEmail());

        if (emailChanged && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists.");
        }

        user.setFullName(request.getFullName());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (emailChanged) {
            user.setEmail(request.getEmail());
        }

        String[] names = splitFullName(request.getFullName());
        keycloakAdminService.updateUser(
                user.getKeycloakId(),
                emailChanged ? request.getEmail() : null,
                names[0],
                names[1]
        );

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password not match");
        }

        try {
            keycloakTokenService.login(user.getEmail(), request.getCurrentPassword());
        } catch (Exception e) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        keycloakAdminService.resetPassword(user.getKeycloakId(), request.getNewPassword());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserNotFoundException();
        }

        String keycloakId = jwt.getSubject();

        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(UserNotFoundException::new);
    }

    private void recordSession(String email, LoginResponse response) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRefreshTokenHash(hash(response.getRefreshToken()));
        refreshToken.setLoginTime(LocalDateTime.now());
        refreshToken.setLastRefreshed(LocalDateTime.now());
        if (response.getExpiresIn() != null) {
            refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresIn()));
        }
        refreshToken.setIsActive(true);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());

        HttpServletRequest httpRequest = currentHttpRequest();
        if (httpRequest != null) {
            refreshToken.setIpAddress(extractClientIp(httpRequest));
            refreshToken.setUserAgent(httpRequest.getHeader("User-Agent"));
            refreshToken.setDeviceType(guessDeviceType(httpRequest.getHeader("User-Agent")));
        }

        refreshTokenRepository.save(refreshToken);
    }

    private HttpServletRequest currentHttpRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String guessDeviceType(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile")) {
            return "MOBILE";
        }
        if (ua.contains("tablet")) {
            return "TABLET";
        }
        return "DESKTOP";
    }

    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{null, null};
        }
        String trimmed = fullName.trim();
        int idx = trimmed.indexOf(' ');
        if (idx < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1).trim()};
    }

    private String hash(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }
}
