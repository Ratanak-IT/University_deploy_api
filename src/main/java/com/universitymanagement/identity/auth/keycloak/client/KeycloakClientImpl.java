package com.universitymanagement.identity.auth.keycloak.client;

import com.universitymanagement.identity.auth.keycloak.KeycloakAdminService;
import com.universitymanagement.identity.auth.keycloak.config.KeycloakProperties;
import com.universitymanagement.identity.exception.KeycloakOperationException;
import com.universitymanagement.identity.exception.KeycloakRoleNotFoundException;
import com.universitymanagement.identity.exception.KeycloakUserNotFoundException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@AllArgsConstructor
public class KeycloakClientImpl implements KeycloakClient{

    private final Keycloak keycloak;
    private final KeycloakProperties properties;


    @Override
    public String createUser(UserRepresentation user) {
        try(Response  response = users().create(user)) {
            validateResponse(response, Response.Status.CREATED);

            return extractUserId(response);
        }
    }

    @Override
    public void deleteUser(String userId) {
        try (Response response = users().delete(userId)) {
            validateResponse(response, Response.Status.NO_CONTENT);
        }
    }

    @Override
    public void assignRealmRole(String userId, String roleName) {
        RoleResource roleResource = realm().roles().get(roleName);

        RoleRepresentation role;
        try {
            role = roleResource.toRepresentation();
        } catch (NotFoundException e) {
            throw new KeycloakRoleNotFoundException("Realm role not found: " + roleName);
        }

        try {
            user(userId).roles().realmLevel().add(List.of(role));
        } catch (ClientErrorException e) {
            throw new KeycloakOperationException(buildErrorMessage("assign realm role", e));
        }
    }

    @Override
    public UserRepresentation findUser(String username) {
        List<UserRepresentation> results = users().search(username, true);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public UserRepresentation findUserById(String userId) {
        try {
            return user(userId).toRepresentation();
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    public void updateUser(UserRepresentation user) {
        try {
            user(user.getId()).update(user);
        } catch (ClientErrorException e) {
            throw new KeycloakOperationException(buildErrorMessage("update user", e));
        }
    }

    @Override
    public void resetPassword(String userId, CredentialRepresentation credential) {
        try {
            user(userId).resetPassword(credential);
        } catch (ClientErrorException e) {
            throw new KeycloakOperationException(buildErrorMessage("reset password", e));
        }
    }

    private String buildErrorMessage(String action, ClientErrorException e) {
        String body = e.getResponse().readEntity(String.class);
        return String.format("Keycloak %s failed. Status=%d, Response=%s",
                action, e.getResponse().getStatus(), body);
    }



    // Helper Methods
    // Keycloak -> Realm
    private RealmResource realm() {
        return keycloak.realm(properties.getRealm());
    }

    // realm -> users
    private UsersResource users() {
        return realm().users();
    }

    // user
    private UserResource user(String userId) {
        return users().get(userId);
    }

    private void validateResponse(Response response, Response.Status exptectedStatus) {
        if(response.getStatus() != exptectedStatus.getStatusCode()) {
            String body = response.readEntity(String.class);
            throw  new KeycloakOperationException(
                    String.format("Keycloak request failed. Expected=%d, Actual=%d, Response=%s",
                            exptectedStatus.getStatusCode(), response.getStatus(), body));
        }

    }

    private String extractUserId(Response response) {
        return CreatedResponseUtil.getCreatedId(response);
    }

}