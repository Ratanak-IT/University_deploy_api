package com.universitymanagement.admin.mapper;

import com.universitymanagement.admin.dto.response.LoginHistoryResponse;
import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.admin.dto.response.UserSummaryResponse;
import com.universitymanagement.identity.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-06T23:49:18+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class AdminUserMapperImpl implements AdminUserMapper {

    @Override
    public UserSummaryResponse toUserSummaryResponse(UserRepresentation userRepresentation) {
        if ( userRepresentation == null ) {
            return null;
        }

        String id = null;
        String username = null;
        String email = null;
        String firstName = null;
        String lastName = null;
        boolean enabled = false;

        id = userRepresentation.getId();
        username = userRepresentation.getUsername();
        email = userRepresentation.getEmail();
        firstName = userRepresentation.getFirstName();
        lastName = userRepresentation.getLastName();
        if ( userRepresentation.isEnabled() != null ) {
            enabled = userRepresentation.isEnabled();
        }

        UserSummaryResponse userSummaryResponse = new UserSummaryResponse( id, username, email, firstName, lastName, enabled );

        return userSummaryResponse;
    }

    @Override
    public UserDetailResponse toUserDetailResponse(UserRepresentation userRepresentation) {
        if ( userRepresentation == null ) {
            return null;
        }

        String id = null;
        String username = null;
        String email = null;
        String firstName = null;
        String lastName = null;
        boolean enabled = false;
        boolean emailVerified = false;
        Long createdTimestamp = null;

        id = userRepresentation.getId();
        username = userRepresentation.getUsername();
        email = userRepresentation.getEmail();
        firstName = userRepresentation.getFirstName();
        lastName = userRepresentation.getLastName();
        if ( userRepresentation.isEnabled() != null ) {
            enabled = userRepresentation.isEnabled();
        }
        if ( userRepresentation.isEmailVerified() != null ) {
            emailVerified = userRepresentation.isEmailVerified();
        }
        createdTimestamp = userRepresentation.getCreatedTimestamp();

        List<String> roles = null;

        UserDetailResponse userDetailResponse = new UserDetailResponse( id, username, email, firstName, lastName, enabled, emailVerified, createdTimestamp, roles );

        return userDetailResponse;
    }

    @Override
    public UserDetailResponse toUserDetailResponse(UserRepresentation userRepresentation, List<String> roles) {
        if ( userRepresentation == null && roles == null ) {
            return null;
        }

        String id = null;
        String username = null;
        String email = null;
        String firstName = null;
        String lastName = null;
        boolean enabled = false;
        boolean emailVerified = false;
        Long createdTimestamp = null;
        if ( userRepresentation != null ) {
            id = userRepresentation.getId();
            username = userRepresentation.getUsername();
            email = userRepresentation.getEmail();
            firstName = userRepresentation.getFirstName();
            lastName = userRepresentation.getLastName();
            if ( userRepresentation.isEnabled() != null ) {
                enabled = userRepresentation.isEnabled();
            }
            if ( userRepresentation.isEmailVerified() != null ) {
                emailVerified = userRepresentation.isEmailVerified();
            }
            createdTimestamp = userRepresentation.getCreatedTimestamp();
        }
        List<String> roles1 = null;
        List<String> list = roles;
        if ( list != null ) {
            roles1 = new ArrayList<String>( list );
        }

        UserDetailResponse userDetailResponse = new UserDetailResponse( id, username, email, firstName, lastName, enabled, emailVerified, createdTimestamp, roles1 );

        return userDetailResponse;
    }

    @Override
    public LoginHistoryResponse toLoginHistoryResponse(RefreshToken refreshToken) {
        if ( refreshToken == null ) {
            return null;
        }

        Long tokenId = null;
        String deviceType = null;
        String ipAddress = null;
        String userAgent = null;
        LocalDateTime loginTime = null;
        LocalDateTime lastRefreshed = null;
        LocalDateTime expiresAt = null;
        LocalDateTime logoutTime = null;
        Boolean isActive = null;
        Boolean revoked = null;

        tokenId = refreshToken.getTokenId();
        deviceType = refreshToken.getDeviceType();
        ipAddress = refreshToken.getIpAddress();
        userAgent = refreshToken.getUserAgent();
        loginTime = refreshToken.getLoginTime();
        lastRefreshed = refreshToken.getLastRefreshed();
        expiresAt = refreshToken.getExpiresAt();
        logoutTime = refreshToken.getLogoutTime();
        isActive = refreshToken.getIsActive();
        revoked = refreshToken.getRevoked();

        LoginHistoryResponse loginHistoryResponse = new LoginHistoryResponse( tokenId, deviceType, ipAddress, userAgent, loginTime, lastRefreshed, expiresAt, logoutTime, isActive, revoked );

        return loginHistoryResponse;
    }
}
