package com.universitymanagement.admin.mapper;

import com.universitymanagement.admin.dto.response.LoginHistoryResponse;
import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.admin.dto.response.UserSummaryResponse;
import com.universitymanagement.identity.entity.RefreshToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    UserSummaryResponse toUserSummaryResponse(UserRepresentation userRepresentation);

    @Mapping(target = "roles", ignore = true)
    UserDetailResponse toUserDetailResponse(UserRepresentation userRepresentation);

    UserDetailResponse toUserDetailResponse(UserRepresentation userRepresentation, List<String> roles);

    LoginHistoryResponse toLoginHistoryResponse(RefreshToken refreshToken);
}
