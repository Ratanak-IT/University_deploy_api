//package com.universitymanagement.config;
//
//import com.universitymanagement.identity.auth.keycloak.config.KeycloakProperties;
//import lombok.RequiredArgsConstructor;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
//import org.springframework.security.web.SecurityFilterChain;
//
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    @Value("${keycloak.client-id}")
//    private String CLIENT_ID;
//    private final KeycloakProperties keycloakProperties;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
//                        .requestMatchers("/swagger-resources/**").permitAll()
//                        .requestMatchers("/webjars/**").permitAll()
//                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh-token").permitAll()
//                        .requestMatchers("/api/v1/auth/**").authenticated()
//                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
//                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
//                        .anyRequest().authenticated()
//                )
//                .oauth2ResourceServer(oauth2 ->
//                        oauth2.jwt(jwt ->
//                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
//                        )
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public JwtAuthenticationConverter jwtAuthenticationConverter() {
//        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
//        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
//        return converter;
//    }
//
//    @SuppressWarnings("unchecked")
//    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
//
//        Set<String> roles = new HashSet<>();
//
//        // 1. Realm roles -> realm_access.roles  (ADMIN, TEACHER, STUDENT នៅទីនេះ)
//        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
//        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
//            realmRoles.forEach(r -> roles.add(String.valueOf(r)));
//        }
//
//        // 2. Client roles -> resource_access.{client-id}.roles
//        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
//        if (resourceAccess != null
//                && resourceAccess.get(CLIENT_ID) instanceof Map<?, ?> client
//                && client.get("roles") instanceof List<?> clientRoles) {
//            clientRoles.forEach(r -> roles.add(String.valueOf(r)));
//        }
//
//        return roles.stream()
//                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
//                .collect(Collectors.toList());
//    }
//}


package com.universitymanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    @Value("${keycloak.client-id}")
    private String CLIENT_ID;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh-token").permitAll()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/departments/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/v1/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/programs/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/v1/programs/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/classrooms/**").authenticated()
                        .requestMatchers("/api/v1/assignments/**", "/api/v1/submissions/**").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

//    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
//
//        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
//
//        if (resourceAccess == null) {
//            return Collections.emptyList();
//        }
//
//        Map<String, Object> client =
//                (Map<String, Object>) resourceAccess.get(CLIENT_ID);
//
//        if (client == null) {
//            return Collections.emptyList();
//        }
//
//        List<String> roles =
//                (List<String>) client.get("roles");
//
//        if (roles == null) {
//            return Collections.emptyList();
//        }
//
//        return roles.stream()
//                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
//                .collect(Collectors.toList());
//    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {

        Set<String> roles = new HashSet<>();

        // 1. Realm roles -> realm_access.roles (ADMIN, TEACHER, STUDENT នៅទីនេះ)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
            realmRoles.forEach(r -> roles.add(String.valueOf(r)));
        }

        // 2. Client roles -> resource_access.{client-id}.roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null
                && resourceAccess.get(CLIENT_ID) instanceof Map<?, ?> client
                && client.get("roles") instanceof List<?> clientRoles) {
            clientRoles.forEach(r -> roles.add(String.valueOf(r)));
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                writeProblem(request, response, HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "Authentication required: missing or invalid access token.",
                        "SEC-401");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeProblem(request, response, HttpStatus.FORBIDDEN,
                        "Access Denied",
                        "You do not have permission to access this resource.",
                        "SEC-403");
    }

    private void writeProblem(HttpServletRequest request,
                              HttpServletResponse response,
                              HttpStatus status,
                              String title,
                              String detail,
                              String errorCode) throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}