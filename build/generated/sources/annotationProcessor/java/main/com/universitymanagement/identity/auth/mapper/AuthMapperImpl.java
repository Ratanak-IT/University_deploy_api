package com.universitymanagement.identity.auth.mapper;

import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;
import com.universitymanagement.identity.auth.keycloak.dto.KeyCloakTokenResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-10T20:49:59+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class AuthMapperImpl implements AuthMapper {

    @Override
    public LoginResponse toLoginResponse(KeyCloakTokenResponse response) {
        if ( response == null ) {
            return null;
        }

        LoginResponse.LoginResponseBuilder loginResponse = LoginResponse.builder();

        loginResponse.accessToken( response.getAccessToken() );
        loginResponse.refreshToken( response.getRefreshToken() );
        loginResponse.tokenType( response.getTokenType() );
        loginResponse.expiresIn( response.getExpiresIn() );
        loginResponse.scope( response.getScope() );

        return loginResponse.build();
    }

    @Override
    public RefreshTokenResponse toRefreshTokenResponse(KeyCloakTokenResponse response) {
        if ( response == null ) {
            return null;
        }

        RefreshTokenResponse refreshTokenResponse = new RefreshTokenResponse();

        refreshTokenResponse.setAccessToken( response.getAccessToken() );
        refreshTokenResponse.setRefreshToken( response.getRefreshToken() );
        refreshTokenResponse.setTokenType( response.getTokenType() );
        refreshTokenResponse.setExpiresIn( response.getExpiresIn() );

        return refreshTokenResponse;
    }
}
