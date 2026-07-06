package com.universitymanagement.identity.auth.mapper;

import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;
import com.universitymanagement.identity.auth.keycloak.dto.KeyCloakTokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

   LoginResponse toLoginResponse(KeyCloakTokenResponse response);
   RefreshTokenResponse toRefreshTokenResponse(KeyCloakTokenResponse response);

}