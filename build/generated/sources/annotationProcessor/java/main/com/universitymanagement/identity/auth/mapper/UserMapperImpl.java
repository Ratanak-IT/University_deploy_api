package com.universitymanagement.identity.auth.mapper;

import com.universitymanagement.identity.auth.dto.request.RegisterRequest;
import com.universitymanagement.identity.auth.dto.response.RegisterResponse;
import com.universitymanagement.identity.auth.dto.response.UserProfileResponse;
import com.universitymanagement.identity.entity.User;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-08T12:21:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(RegisterRequest request) {
        if ( request == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( request.getEmail() );
        user.setPhoneNumber( request.getPhoneNumber() );
        user.setGender( request.getGender() );

        return user;
    }

    @Override
    public User toEntity(UserRepresentation kc) {
        if ( kc == null ) {
            return null;
        }

        User user = new User();

        if ( kc.getId() != null ) {
            user.setId( UUID.fromString( kc.getId() ) );
        }
        user.setEmail( kc.getEmail() );

        return user;
    }

    @Override
    public RegisterResponse toRegisterResponse(User user) {
        if ( user == null ) {
            return null;
        }

        RegisterResponse.RegisterResponseBuilder registerResponse = RegisterResponse.builder();

        registerResponse.id( user.getId() );
        registerResponse.keycloakId( user.getKeycloakId() );
        registerResponse.email( user.getEmail() );
        registerResponse.isActive( user.getIsActive() );
        registerResponse.accountStatus( user.getAccountStatus() );
        registerResponse.createdAt( user.getCreatedAt() );

        return registerResponse.build();
    }

    @Override
    public UserProfileResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserProfileResponse.UserProfileResponseBuilder userProfileResponse = UserProfileResponse.builder();

        userProfileResponse.id( user.getId() );
        userProfileResponse.keycloakId( user.getKeycloakId() );
        userProfileResponse.email( user.getEmail() );
        userProfileResponse.fullName( user.getFullName() );
        userProfileResponse.phoneNumber( user.getPhoneNumber() );
        userProfileResponse.dateOfBirth( user.getDateOfBirth() );
        userProfileResponse.gender( user.getGender() );
        userProfileResponse.isActive( user.getIsActive() );

        return userProfileResponse.build();
    }
}
