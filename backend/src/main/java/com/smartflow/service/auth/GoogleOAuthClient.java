package com.smartflow.service.auth;

import com.smartflow.exception.ErrorCode;
import com.smartflow.exception.SmartflowException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthClient {

    private static final List<String> REQUIRED_SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/gmail.compose",
            "https://www.googleapis.com/auth/calendar",
            "https://www.googleapis.com/auth/drive.readonly",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "openid"
    );

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String defaultRedirectUri;

    @Value("${google.oauth.token-uri}")
    private String tokenUri;

    @Value("${google.oauth.userinfo-uri}")
    private String userinfoUri;

    private final WebClient webClient = WebClient.create();

    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", defaultRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", REQUIRED_SCOPES))
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @SuppressWarnings("unchecked")
    public TokenResponse exchangeCodeForTokens(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            Map<String, Object> response = webClient.post()
                    .uri(tokenUri)
                    .body(BodyInserters.fromFormData(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) throw new SmartflowException(ErrorCode.AUTH_003);

            return TokenResponse.builder()
                    .accessToken((String) response.get("access_token"))
                    .refreshToken((String) response.get("refresh_token"))
                    .expiresIn(response.get("expires_in") instanceof Number n ? n.longValue() : 3600L)
                    .build();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 400) throw new SmartflowException(ErrorCode.AUTH_002);
            throw new SmartflowException(ErrorCode.AUTH_003);
        }
    }

    @SuppressWarnings("unchecked")
    public UserInfo getUserInfo(String accessToken) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(userinfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) throw new SmartflowException(ErrorCode.AUTH_003);

            return UserInfo.builder()
                    .googleId((String) response.get("sub"))
                    .email((String) response.get("email"))
                    .name((String) response.get("name"))
                    .profileImageUrl((String) response.get("picture"))
                    .build();
        } catch (WebClientResponseException e) {
            throw new SmartflowException(ErrorCode.AUTH_003);
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final long expiresIn;
    }

    @lombok.Builder
    @lombok.Getter
    public static class UserInfo {
        private final String googleId;
        private final String email;
        private final String name;
        private final String profileImageUrl;
    }
}
