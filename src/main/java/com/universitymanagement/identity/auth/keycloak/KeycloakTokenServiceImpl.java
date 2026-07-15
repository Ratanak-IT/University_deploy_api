package com.universitymanagement.identity.auth.keycloak;

import com.universitymanagement.identity.exception.InvalidAuthorizationCodeException;
import com.universitymanagement.identity.exception.InvalidCredentialsException;
import com.universitymanagement.identity.exception.InvalidRefreshTokenException;
import com.universitymanagement.identity.exception.KeycloakUnavailableException;
import com.universitymanagement.identity.auth.dto.request.LogoutRequest;
import com.universitymanagement.identity.auth.dto.request.RefreshTokenRequest;
import com.universitymanagement.identity.auth.dto.response.LoginResponse;
import com.universitymanagement.identity.auth.dto.response.RefreshTokenResponse;
import com.universitymanagement.identity.auth.mapper.AuthMapper;
import com.universitymanagement.identity.auth.keycloak.config.KeycloakProperties;
import com.universitymanagement.identity.auth.keycloak.dto.KeyCloakTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;


@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakTokenServiceImpl implements KeycloakTokenService{

    private static final String TOKEN_ENDPOINT = "/realms/{realm}/protocol/openid-connect/token";
    private static final String LOGOUT_ENDPOINT = "/realms/{realm}/protocol/openid-connect/logout";

    private final RestClient client;
    private final KeycloakProperties properties;
    private final AuthMapper authMapper;

    @Override
    public LoginResponse login(String email, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.getClientId());
        form.add("username", email);
        form.add("password", password);

        if (properties.getClientSecret() != null) {
            form.add("client_secret", properties.getClientSecret());
        }

        KeyCloakTokenResponse tokenResponse = client.post()
                .uri(TOKEN_ENDPOINT, properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Keycloak token error: status={} body={}", res.getStatusCode(), body);
                    throw new InvalidCredentialsException("Keycloak: " + body); // បង្ហាញបណ្ដោះអាសន្នដើម្បី debug
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new KeycloakUnavailableException("Keycloak server error");
                })
                .body(KeyCloakTokenResponse.class);

        return authMapper.toLoginResponse(tokenResponse);
    }

    @Override
    public LoginResponse exchangeAuthorizationCode(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code_verifier", codeVerifier);

        if(properties.getClientSecret() != null) {
            form.add("client_secret", properties.getClientSecret());
        }

        KeyCloakTokenResponse  tokenResponse  = client.post()
                .uri(TOKEN_ENDPOINT, properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req , res) ->  {
                    try {
                        throw  new InvalidAuthorizationCodeException("Fail to exchange authorization code");
                    } catch (InvalidAuthorizationCodeException e) {
//                        throw new RuntimeException(e);
                    }

                })
                .onStatus(HttpStatusCode::is5xxServerError , (req , res) -> {
        throw new KeycloakUnavailableException("Keycloak server error");
        })
        .body(KeyCloakTokenResponse.class);

        return authMapper.toLoginResponse(tokenResponse);
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.getClientId());
        form.add("refresh_token", request.refreshToken());
        if (properties.getClientSecret() != null) {
            form.add("client_secret", properties.getClientSecret());
        }

        KeyCloakTokenResponse tokenResponse = client.post()
                .uri(TOKEN_ENDPOINT, properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new InvalidRefreshTokenException("Refresh token invalid or expired");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new KeycloakUnavailableException("Keycloak server error");
                })
                .body(KeyCloakTokenResponse.class);

        return authMapper.toRefreshTokenResponse(tokenResponse);
    }

    @Override
    public void logout(LogoutRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("refresh_token", request.getRefreshToken());

        if(properties.getClientSecret() != null) {
            form.add("client_secret" , properties.getClientSecret());
        }
        client.post()
                .uri(LOGOUT_ENDPOINT, properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new InvalidRefreshTokenException("Failed to logout, invalid token");
                })
                .toBodilessEntity();

    }
}
