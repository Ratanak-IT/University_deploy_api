package com.universitymanagement.identity.auth.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionResponse {
    private  Long id;
    private String permissionName;
    private String description;

}
