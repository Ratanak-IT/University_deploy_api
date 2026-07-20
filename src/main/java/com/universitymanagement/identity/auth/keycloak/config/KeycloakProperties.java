package com.universitymanagement.identity.auth.keycloak.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakProperties {

    private String serverUrl;
    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String scope = "openid profile email";

    @Value("${keycloak.admin-client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin-client-secret:}")
    private String adminClientSecret;

    @Value("${keycloak.admin-username:}")
    private String adminUsername;

    @Value("${keycloak.admin-password:}")
    private String adminPassword;

}