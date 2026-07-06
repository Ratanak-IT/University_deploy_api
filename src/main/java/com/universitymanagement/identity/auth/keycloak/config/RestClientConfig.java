package com.universitymanagement.identity.auth.keycloak.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig  {

    @Bean
    public RestClient restClient(RestClient.Builder builder, KeycloakProperties properties){

        return builder
                .baseUrl(properties.getServerUrl())
                .build();
    }

}

// restClient.post()
//    .uri("/realms/{realm}/protocol/openid-connect/token",
//         properties.getRealm())