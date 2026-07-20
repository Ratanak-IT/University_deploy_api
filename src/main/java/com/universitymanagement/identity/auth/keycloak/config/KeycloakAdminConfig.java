package com.universitymanagement.identity.auth.keycloak.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloak(KeycloakProperties properties) {

        KeycloakBuilder builder = KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getRealm())
                .clientId(properties.getAdminClientId());

        boolean hasAdminUser = properties.getAdminUsername() != null
                && !properties.getAdminUsername().isBlank();

        if (hasAdminUser) {
            builder.grantType(OAuth2Constants.PASSWORD)
                    .username(properties.getAdminUsername())
                    .password(properties.getAdminPassword());
            if (properties.getAdminClientSecret() != null
                    && !properties.getAdminClientSecret().isBlank()) {
                builder.clientSecret(properties.getAdminClientSecret());
            }
        } else {
            builder.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientSecret(properties.getAdminClientSecret());
        }

        return builder.build();
    }
}