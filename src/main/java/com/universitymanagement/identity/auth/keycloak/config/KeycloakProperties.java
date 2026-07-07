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

    // Token API
    private String clientId;

    private String clientSecret;

    private String redirectUri;

}