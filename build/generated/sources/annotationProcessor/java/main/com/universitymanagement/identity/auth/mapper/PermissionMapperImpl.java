package com.universitymanagement.identity.auth.mapper;

import com.universitymanagement.identity.auth.dto.response.PermissionResponse;
import com.universitymanagement.identity.entity.Permission;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-06T13:19:52+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.5.jar, environment: Java 25.0.3 (Oracle Corporation)"
)
@Component
public class PermissionMapperImpl implements PermissionMapper {

    @Override
    public PermissionResponse toResponse(Permission permission) {
        if ( permission == null ) {
            return null;
        }

        PermissionResponse.PermissionResponseBuilder permissionResponse = PermissionResponse.builder();

        permissionResponse.id( permission.getId() );
        permissionResponse.description( permission.getDescription() );

        return permissionResponse.build();
    }
}
