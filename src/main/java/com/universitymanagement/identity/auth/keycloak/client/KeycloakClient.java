package com.universitymanagement.identity.auth.keycloak.client;


import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public interface KeycloakClient {

    // KeycloakClientRealmResource Response (UsersResource, UserRepresentation, Response,RoleRepresentation),

    String createUser(UserRepresentation user);

    void deleteUser(String userId);

    void assignRealmRole(String userId, String roleName);

    UserRepresentation findUser(String username);

    UserRepresentation findUserById(String userId);

    void updateUser(UserRepresentation user);

    void resetPassword(String userId, CredentialRepresentation credential);

}