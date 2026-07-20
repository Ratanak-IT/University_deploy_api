package com.universitymanagement.identity.auth.mapper;

import com.universitymanagement.identity.auth.dto.response.PermissionResponse;
import com.universitymanagement.identity.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);
}
