package com.universitymanagement.identity.auth.mapper;

import com.universitymanagement.identity.auth.dto.response.RoleResponse;
import com.universitymanagement.identity.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}