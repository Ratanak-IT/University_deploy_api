package com.universitymanagement.identity.auth.keycloak;

import com.universitymanagement.identity.auth.dto.request.RegisterRequest;
import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;

public interface KeycloakAdminService {

    //    This is your business/service layer
    String createUser(RegisterRequest request);

    void assignRole(String userId, String roleName);

    void resetPassword(String userId, String password);

    void disableUser(String userId);

    void enableUser(String userId);

    void deleteUser(String userId);

    UserProfileResponse getUser(String username);

    void updateUser(String userId, String email, String firstName, String lastName);

}
