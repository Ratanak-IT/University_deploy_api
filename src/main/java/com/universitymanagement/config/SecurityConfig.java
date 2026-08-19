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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String ADMIN = "ADMIN";
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";

    private final ObjectMapper objectMapper;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Preflight CORS Request Handlers
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. Swagger / OpenAPI Documentation Resources
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // 3. Public Authentication & Identity Endpoints
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/callback",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh-token"
                        ).permitAll()

                        // 4. Public Academic Read Operations
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/programs", "/api/v1/programs/**",
                                "/api/v1/curriculums", "/api/v1/curriculums/**",
                                "/api/v1/departments", "/api/v1/departments/**",
                                "/api/v1/subjects", "/api/v1/subjects/**"
                        ).permitAll()

                        // 5. Admin-Only Management Endpoints
                        .requestMatchers(
                                "/api/v1/users/**",
                                "/api/v1/admin/**",
                                "/api/v1/students/admin/**"
                        ).hasRole(ADMIN)
                        .requestMatchers(
                                HttpMethod.POST, "/api/v1/teachers/**", "/api/v1/departments/**", "/api/v1/programs/**", "/api/v1/subjects/**", "/api/v1/curriculums/**"
                        ).hasRole(ADMIN)
                        .requestMatchers(
                                HttpMethod.PUT, "/api/v1/teachers/**", "/api/v1/departments/**", "/api/v1/programs/**", "/api/v1/subjects/**", "/api/v1/curriculums/**"
                        ).hasRole(ADMIN)
                        .requestMatchers(
                                HttpMethod.DELETE, "/api/v1/teachers/**", "/api/v1/departments/**", "/api/v1/programs/**", "/api/v1/subjects/**", "/api/v1/curriculums/**"
                        ).hasRole(ADMIN)

                        .requestMatchers(
                                HttpMethod.POST, "/api/v1/assignments/*/submissions"
                        ).hasRole(STUDENT)
                        .requestMatchers("/api/v1/submissions/**").hasAnyRole(ADMIN, TEACHER, STUDENT)


                        .requestMatchers(
                                "/api/v1/classrooms/*/comments",
                                "/api/v1/classrooms/*/comments/**",
                                "/api/v1/classrooms/*/mentionable-members",
                                "/api/v1/assignments/*/comments",
                                "/api/v1/assignments/*/comments/**",
                                "/api/v1/assignments/*/mentionable-members",
                                "/api/v1/comments/**"
                        ).hasAnyRole(ADMIN, TEACHER, STUDENT)

                        // 7. Teacher & Admin Write Operations (Create, Update, Delete for Academic Resources)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/classrooms/**",
                                "/api/v1/lessons/**",
                                "/api/v1/assignments/**",
                                "/api/v1/quizzes/**",
                                "/api/v1/scores/**",
                                "/api/v1/attendance/**",
                                "/api/v1/certificates/**"
                        ).hasAnyRole(TEACHER, ADMIN)
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/classrooms/**",
                                "/api/v1/lessons/**",
                                "/api/v1/assignments/**",
                                "/api/v1/quizzes/**",
                                "/api/v1/scores/**",
                                "/api/v1/attendance/**",
                                "/api/v1/certificates/**"
                        ).hasAnyRole(TEACHER, ADMIN)
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/classrooms/**",
                                "/api/v1/lessons/**",
                                "/api/v1/assignments/**",
                                "/api/v1/quizzes/**",
                                "/api/v1/scores/**",
                                "/api/v1/attendance/**",
                                "/api/v1/certificates/**"
                        ).hasAnyRole(TEACHER, ADMIN)
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/classrooms/**",
                                "/api/v1/lessons/**",
                                "/api/v1/assignments/**",
                                "/api/v1/quizzes/**",
                                "/api/v1/scores/**",
                                "/api/v1/attendance/**",
                                "/api/v1/certificates/**"
                        ).hasAnyRole(TEACHER, ADMIN)

                        // 8. Read-Only Access (GET) for Authenticated Users (ADMIN, TEACHER, STUDENT)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/classrooms/**",
                                "/api/v1/lessons/**",
                                "/api/v1/assignments/**",
                                "/api/v1/quizzes/**",
                                "/api/v1/scores/**",
                                "/api/v1/attendance/**",
                                "/api/v1/certificates/**",
                                "/api/v1/notifications/**",
                                "/api/v1/students/**",
                                "/api/v1/teachers/**"
                        ).hasAnyRole(ADMIN, TEACHER, STUDENT)

                        // 9. Catch-All for any other authenticated route
                        .anyRequest().hasAnyRole(ADMIN, TEACHER, STUDENT)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(ex -> ex
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

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        // Extract Keycloak Realm Roles (e.g., ADMIN, TEACHER, STUDENT)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
            realmRoles.forEach(r -> roles.add(String.valueOf(r)));
        }

        // Extract Keycloak Client Roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null
                && resourceAccess.get(clientId) instanceof Map<?, ?> client
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
                              String errorCode) throws IOException {
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
