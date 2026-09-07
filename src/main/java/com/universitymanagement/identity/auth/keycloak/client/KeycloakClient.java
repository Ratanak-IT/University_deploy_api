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

    /**
     * Ends every session this user currently has.
     *
     * <p>Disabling an account stops Keycloak issuing anything new, but it does
     * not touch what is already out there: the browser keeps its session cookie
     * and the app keeps a working access token until it expires. For a
     * suspension that means the person carries on as though nothing happened
     * for the rest of the token's life. This closes it immediately.
     */
    void logoutAllSessions(String userId);

}