package com.universitymanagement.admin.serviceImpl;

import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
import com.universitymanagement.admin.dto.request.UpdateStatusRequest;
import com.universitymanagement.admin.dto.response.LoginHistoryResponse;
import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.admin.dto.response.UserSummaryResponse;
import com.universitymanagement.admin.mapper.AdminUserMapper;
import com.universitymanagement.admin.service.UserManageService;
import com.universitymanagement.identity.auth.dto.request.CreateUserRequest;
import com.universitymanagement.identity.auth.dto.request.UpdateUserRequest;
import com.universitymanagement.identity.auth.dto.response.CreateUserResponse;

import com.universitymanagement.identity.auth.keycloak.client.KeycloakClient;
import com.universitymanagement.identity.entity.AccountStatus;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.DuplicateResourceException;
import com.universitymanagement.identity.repository.RefreshTokenRepository;
import com.universitymanagement.identity.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManageServiceImpl implements UserManageService {
    private final Keycloak keycloak;
    private final AdminUserMapper userMapper;
    private final KeycloakClient keycloakClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public Page<UserSummaryResponse> findAllUsers(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        List<UserRepresentation> keycloakUsers = keycloak
                .realm(realm)
                .users()
                .list(page * size, size);

        List<UserSummaryResponse> content = keycloakUsers.stream()
                .map(userMapper::toUserSummaryResponse)
                .toList();

        long total = keycloak
                .realm(realm)
                .users()
                .count();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public UserDetailResponse findUserById(String id) {
        UserRepresentation user = requireKeycloakUser(id);
        List<String> roles = fetchRealmRoles(id);
        return userMapper.toUserDetailResponse(user, roles);
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password not match");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists.");
        }

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(request.email());
        kcUser.setEmail(request.email());
        kcUser.setFirstName(request.firstName());
        kcUser.setLastName(request.lastName());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);
        kcUser.setCredentials(List.of(credential));

        String keycloakId = keycloakClient.createUser(kcUser);
        keycloakClient.assignRealmRole(keycloakId, request.role().name());

        User user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setKeycloakId(keycloakId);
        user.setEmail(request.email());
        user.setFullName((request.firstName() + " " + request.lastName()).trim());
        user.setPhone(request.phoneNumber());
        user.setAccountStatus(AccountStatus.ACTIVE.name());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        return new CreateUserResponse(
                savedUser.getId(),
                keycloakId,
                request.email(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.role(),
                true
        );
    }

    @Override
    public UserDetailResponse updateUser(String id, UpdateUserRequest request) {
        UserRepresentation kcUser = requireKeycloakUser(id);

        if (request.email() != null && !request.email().isBlank()) {
            kcUser.setEmail(request.email());
        }
        if (request.firstName() != null && !request.firstName().isBlank()) {
            kcUser.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            kcUser.setLastName(request.lastName());
        }

        keycloakClient.updateUser(kcUser);

        userRepository.findByKeycloakId(id).ifPresent(user -> {
            if (request.email() != null && !request.email().isBlank()) {
                user.setEmail(request.email());
            }
            if (request.firstName() != null || request.lastName() != null) {
                String first = request.firstName() != null ? request.firstName() : "";
                String last = request.lastName() != null ? request.lastName() : "";
                String fullName = (first + " " + last).trim();
                if (!fullName.isBlank()) {
                    user.setFullName(fullName);
                }
            }
            if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
                user.setPhone(request.phoneNumber());
            }
            userRepository.save(user);
        });

        return userMapper.toUserDetailResponse(kcUser, fetchRealmRoles(id));
    }

    @Override
    public void updateStatus(String id, UpdateStatusRequest request) {
        boolean enabled = request.status() == AccountStatus.ACTIVE;

        syncKeycloakAndLocalUser(id,
                kcUser -> kcUser.setEnabled(enabled),
                localUser -> {
                    localUser.setAccountStatus(request.status().name());
                    localUser.setIsActive(enabled);
                });
    }

    @Override
    public void resetPassword(String id, AdminResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password not match");
        }
        requireKeycloakUser(id);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.newPassword());
        credential.setTemporary(request.temporary());

        keycloakClient.resetPassword(id, credential);
    }

    @Override
    public List<LoginHistoryResponse> getLoginHistory(String id) {
        User user = userRepository.findByKeycloakId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        return refreshTokenRepository.findByUser_IdOrderByLoginTimeDesc(user.getId())
                .stream()
                .map(userMapper::toLoginHistoryResponse)
                .toList();
    }

    @Override
    public void deleteUser(String id) {
        requireKeycloakUser(id);
        keycloakClient.deleteUser(id);
        userRepository.findByKeycloakId(id).ifPresent(userRepository::delete);
    }

    @Override
    public void softDeleteUser(String id) {
        syncKeycloakAndLocalUser(id,
                kcUser -> {
                    kcUser.setEnabled(false);
                    kcUser.singleAttribute("deleted", "true");
                },
                localUser -> {
                    localUser.setIsActive(false);
                    localUser.setAccountStatus(AccountStatus.SUSPENDED.name());
                });
    }

    @Override
    public void restoreUser(String id) {
        syncKeycloakAndLocalUser(id,
                kcUser -> {
                    kcUser.setEnabled(true);
                    if (kcUser.getAttributes() != null) {
                        kcUser.getAttributes().remove("deleted");
                    }
                },
                localUser -> {
                    localUser.setIsActive(true);
                    localUser.setAccountStatus(AccountStatus.ACTIVE.name());
                });
    }

    /**
     * Shared pattern used by updateStatus / softDeleteUser / restoreUser:
     * 1) load the Keycloak user, 2) apply Keycloak-side changes, 3) push update to Keycloak,
     * 4) apply the matching local-DB changes if a local record exists.
     */
    private void syncKeycloakAndLocalUser(
            String id,
            java.util.function.Consumer<UserRepresentation> keycloakMutation,
            java.util.function.Consumer<User> localMutation) {

        UserRepresentation kcUser = requireKeycloakUser(id);
        keycloakMutation.accept(kcUser);
        keycloakClient.updateUser(kcUser);

        userRepository.findByKeycloakId(id).ifPresent(localUser -> {
            localMutation.accept(localUser);
            userRepository.save(localUser);
        });
    }

    private UserRepresentation requireKeycloakUser(String id) {
        UserRepresentation user = keycloakClient.findUserById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        return user;
    }

    private List<String> fetchRealmRoles(String id) {
        try {
            return keycloak.realm(realm)
                    .users()
                    .get(id)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .toList();
        } catch (NotFoundException e) {
            return List.of();
        }
    }
}