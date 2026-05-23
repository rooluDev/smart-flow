package com.smartflow.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserDto user;

    @Getter
    @Builder
    public static class UserDto {
        private final Long id;
        private final String email;
        private final String name;
        private final String profileImageUrl;
    }

    @Getter
    @Builder
    public static class TokenRefreshDto {
        private final String accessToken;
        private final String tokenType;
        private final long expiresIn;
    }
}
