package com.universitymanagement.identity.auth.keycloak;

import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;
import com.universitymanagement.identity.auth.keycloak.Mapper.KeycloakMapper;
import com.universitymanagement.identity.auth.keycloak.client.KeycloakClient;
import com.universitymanagement.identity.exception.KeycloakUserNotFoundException;
import com.universitymanagement.identity.auth.dto.request.RegisterRequest;
import com.universitymanagement.identity.auth.keycloak.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeycloakAdminServiceImpl  implements KeycloakAdminService {

    private final KeycloakProperties properties;
    private final KeycloakClient client;
    private final KeycloakMapper mapper;

    @Override
    public String createUser(RegisterRequest request) {
        UserRepresentation user = mapper.toUserRepresentation(request);
        return client.createUser(user);
    }

    @Override
    public void assignRole(String userId, String roleName) {
        client.assignRealmRole(userId, roleName);
    }

    @Override
    public void resetPassword(String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        client.resetPassword(userId, credential);
    }

    @Override
    public void disableUser(String userId) {
        UserRepresentation user = requireUser(userId);
        user.setEnabled(false);
        client.updateUser(user);
    }

    @Override
    public void enableUser(String userId) {
        UserRepresentation user = requireUser(userId);
        user.setEnabled(true);
        client.updateUser(user);
    }

    @Override
    public void deleteUser(String userId) {
        client.deleteUser(userId);
    }

    @Override
    public UserProfileResponse getUser(String username) {
        UserRepresentation user = client.findUser(username);
        if (user == null) {
            throw new KeycloakUserNotFoundException("Keycloak user not found: " + username);
        }

        return UserProfileResponse.builder()
                .keycloakId(user.getId())
                .email(user.getEmail())
                .fullName(buildFullName(user))
                .isActive(user.isEnabled())
                .build();
    }

    @Override
    public void updateUser(String userId, String email, String firstName, String lastName) {
        UserRepresentation user = requireUser(userId);

        if (email != null && !email.isBlank()) {
            user.setEmail(email);
            user.setUsername(email);
        }
        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName);
        }

        client.updateUser(user);
    }

    private UserRepresentation requireUser(String userId) {
        UserRepresentation user = client.findUserById(userId);
        if (user == null) {
            throw new KeycloakUserNotFoundException("Keycloak user not found: " + userId);
        }
        return user;
    }

    private String buildFullName(UserRepresentation user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String fullName = (first + " " + last).trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
