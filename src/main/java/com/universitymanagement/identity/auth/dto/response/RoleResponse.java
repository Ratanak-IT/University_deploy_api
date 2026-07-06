package com.universitymanagement.identity.auth.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse {
    private Long Id;
    private String roleName;
}
