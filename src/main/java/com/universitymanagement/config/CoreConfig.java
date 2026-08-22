package com.universitymanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CoreConfig {

    /**
     * Browser origins allowed to call the API directly.
     *
     * <p>These were hard-coded to localhost, which is fine while the admin app
     * proxies every request through its own server — but silently blocks the
     * browser the moment it talks to the API directly from a real domain. Set
     * {@code APP_CORS_ALLOWED_ORIGINS} on the server to the deployed front-end
     * origins, comma-separated.
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        // Credentials are allowed, so the origin list must stay explicit —
        // a wildcard with credentials is rejected by every browser, and would
        // be the wrong thing to want here anyway.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
