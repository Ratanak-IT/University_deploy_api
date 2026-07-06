package com.universitymanagement.identity.auth.keycloak.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakProperties {

    private String serverUrl;

    private String realm;

    // Token API
    private String clientId;

    private String clientSecret;

    private String redirectUri;

}