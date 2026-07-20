package com.universitymanagement.identity.auth.mapper;

import com.universitymanagement.identity.auth.dto.request.RegisterRequest;
import com.universitymanagement.identity.auth.dto.response.RegisterResponse;
import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;
import com.universitymanagement.identity.entity.User;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Convert from RegisterRequest to User Entity
    User toEntity(RegisterRequest request);

    // Convert Keycloak's UserRepresentation into a User entity (used by KeycloakSyncService)
    User toEntity(UserRepresentation kc);


    //Convert from User Entity to RegisterResponse

    RegisterResponse toRegisterResponse(User user);


    // Convert UserEntity to UserProfileResponse
    UserProfileResponse toResponse(User user);


}
